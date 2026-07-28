package portal.expenses.service;

import portal.expenses.entity.ApprovalStatus;
import portal.expenses.dto.ExpenseFilterRequest;
import portal.expenses.dto.PageResponse;
import portal.expenses.entity.AppUser;
import portal.expenses.entity.AuditEntry;
import portal.expenses.entity.Expense;
import portal.expenses.entity.ExpenseCategory;
import portal.expenses.repository.AuditEntryRepository;
import portal.expenses.repository.ExpenseRepository;
import portal.expenses.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ExpenseService {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseService.class);
    private static final String USER_NOT_FOUND_PREFIX = "User not found: ";
    private static final String EXPENSE_NOT_FOUND_PREFIX = "Expense not found with ID: ";

    @Value("${storage.mode}")
    private String storageMode;

    private ExpenseService self;

    @Autowired
    public void setSelf(@Lazy ExpenseService self) {
        this.self = self;
    }

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final LocalStorageService localStorageService;
    private final PolicyEngineService policyEngineService;
    private final AuditEntryRepository auditEntryRepository;

    public ExpenseService(ExpenseRepository expenseRepository, UserRepository userRepository,
                         S3Service s3Service, LocalStorageService localStorageService,
                         PolicyEngineService policyEngineService, AuditEntryRepository auditEntryRepository) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.s3Service = s3Service;
        this.localStorageService = localStorageService;
        this.policyEngineService = policyEngineService;
        this.auditEntryRepository = auditEntryRepository;
    }

    @Transactional
    public Expense createExpense(String description, BigDecimal amount, MultipartFile receipt,
                                 String username, ExpenseCategory category, LocalDate expenseDate,
                                 boolean draft) throws IOException {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_PREFIX + username));

        // Upload receipt based on storage mode
        String fileKey = null;
        boolean hasReceipt = receipt != null && !receipt.isEmpty();

        if (hasReceipt) {
            fileKey = "receipts/" + user.getId() + "/" + UUID.randomUUID() + "-" + receipt.getOriginalFilename();

            // Upload to the configured storage
            if ("s3".equalsIgnoreCase(storageMode)) {
                s3Service.uploadFile(fileKey, receipt.getBytes());
                logger.debug("Receipt uploaded to S3: {}", fileKey);
            } else {
                localStorageService.uploadFile(fileKey, receipt.getBytes());
                logger.debug("Receipt uploaded to local storage: {}", fileKey);
            }
        }

        // Create expense entity
        Expense expense = new Expense();
        expense.setDescription(description);
        expense.setAmount(amount);
        expense.setUser(user);
        expense.setS3ObjectKey(fileKey);  // Store the file key regardless of storage mode
        expense.setCategory(category != null ? category : ExpenseCategory.OTHER);
        expense.setExpenseDate(expenseDate != null ? expenseDate : LocalDate.now());
        expense.setHasReceipt(hasReceipt);

        // If draft, set status to DRAFT and skip policy evaluation
        if (draft) {
            expense.setApprovalStatus(ApprovalStatus.DRAFT);
            expense = expenseRepository.save(expense);

            // Create audit entry for draft creation
            createAuditEntryWithStatus(expense, user, "CREATED",
                null, "DRAFT", "Expense created as draft");

            logger.info("Expense created as draft: ID={}, user={}", expense.getId(), user.getUsername());
            return expense;
        }

        // Save expense first to get an ID
        expense = expenseRepository.save(expense);

        // Evaluate policy rules and determine approval status
        PolicyEngineService.ApprovalDecision decision = policyEngineService.evaluateExpense(expense);
        expense.setApprovalStatus(decision.getFinalStatus());
        expense.setPolicyFlags(decision.getReason());

        // Update expense with the determined status
        expense = expenseRepository.save(expense);

        // Create audit entry for expense creation
        createAuditEntryWithStatus(expense, user, "CREATED",
            null, expense.getApprovalStatus().toString(),
            "Expense created with status: " + decision.getFinalStatus());

        // Create audit entry for policy check
        createAuditEntry(expense, user, "POLICY_CHECK",
            String.format("Policy evaluated: %s - Status: %s",
                decision.getReason(), decision.getFinalStatus()));

        logger.info("Expense created: ID={}, status={}, user={}",
                   expense.getId(), decision.getFinalStatus(), user.getUsername());

        return expense;
    }

    // Overload for backward compatibility
    @Transactional
    public Expense createExpense(String description, BigDecimal amount, MultipartFile receipt,
                                 String username, ExpenseCategory category, LocalDate expenseDate) throws IOException {
        return self.createExpense(description, amount, receipt, username, category, expenseDate, false);
    }

    public List<Expense> getExpensesForUser(Long userId) {
        return expenseRepository.findByUserId(userId);
    }

    public Expense getExpenseById(Long expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new NoSuchElementException(EXPENSE_NOT_FOUND_PREFIX + expenseId));
    }

    public List<Expense> getExpensesForCurrentUser(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_PREFIX + username));
        return expenseRepository.findByUserId(user.getId());
    }

    /**
     * Submit an expense (change from DRAFT to SUBMITTED, then evaluate policies)
     */
    @Transactional
    public Expense submitExpense(Long expenseId, String username) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new NoSuchElementException(EXPENSE_NOT_FOUND_PREFIX + expenseId));

        // Verify ownership
        if (!expense.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("User is not authorized to submit this expense");
        }

        // Validate that expense is in DRAFT status (or PENDING for backward compatibility)
        if (expense.getApprovalStatus() != ApprovalStatus.DRAFT
            && expense.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException(
                "Only DRAFT or PENDING expenses can be submitted. Current status: " + expense.getApprovalStatus());
        }

        // Capture old status before evaluation
        ApprovalStatus oldStatus = expense.getApprovalStatus();

        // Evaluate policy rules and determine approval status
        PolicyEngineService.ApprovalDecision decision = policyEngineService.evaluateExpense(expense);
        expense.setApprovalStatus(decision.getFinalStatus());
        expense.setSubmittedAt(java.time.LocalDateTime.now());

        expense = expenseRepository.save(expense);

        // Create audit entry for submission
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_PREFIX + username));
        createAuditEntryWithStatus(expense, user, "SUBMITTED",
            oldStatus != null ? oldStatus.toString() : null,
            expense.getApprovalStatus().toString(),
            "Expense submitted for approval");

        logger.info("Expense submitted: ID={}, status={}, user={}",
                   expense.getId(), decision.getFinalStatus(), username);

        return expense;
    }

    /**
     * Update a draft expense (only DRAFT expenses can be updated)
     */
    @Transactional
    public Expense updateExpense(Long expenseId, String description, BigDecimal amount,
                                 MultipartFile receipt, String username, ExpenseCategory category,
                                 LocalDate expenseDate) throws IOException {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new NoSuchElementException(EXPENSE_NOT_FOUND_PREFIX + expenseId));

        // Verify ownership
        if (!expense.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("User is not authorized to update this expense");
        }

        // Only allow updates on DRAFT expenses
        if (expense.getApprovalStatus() != ApprovalStatus.DRAFT) {
            throw new IllegalStateException(
                "Only DRAFT expenses can be updated. Current status: " + expense.getApprovalStatus());
        }

        // Update fields
        if (description != null) {
            expense.setDescription(description);
        }
        if (amount != null) {
            expense.setAmount(amount);
        }
        if (category != null) {
            expense.setCategory(category);
        }
        if (expenseDate != null) {
            expense.setExpenseDate(expenseDate);
        }

        // Handle receipt upload if provided
        if (receipt != null && !receipt.isEmpty()) {
            String fileKey = "receipts/" + expense.getUser().getId() + "/" +
                           UUID.randomUUID() + "-" + receipt.getOriginalFilename();

            if ("s3".equalsIgnoreCase(storageMode)) {
                s3Service.uploadFile(fileKey, receipt.getBytes());
            } else {
                localStorageService.uploadFile(fileKey, receipt.getBytes());
            }

            expense.setS3ObjectKey(fileKey);
            expense.setHasReceipt(true);
        }

        expense = expenseRepository.save(expense);

        // Create audit entry for update
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_PREFIX + username));
        createAuditEntry(expense, user, "UPDATED",
            String.format("Expense updated: amount=%s, category=%s", amount, category));

        logger.info("Expense updated: ID={}, user={}", expenseId, username);
        return expense;
    }

    /**
     * Get expenses by approval status
     */
    public List<Expense> getExpensesByStatus(ApprovalStatus status) {
        return expenseRepository.findByApprovalStatus(status);
    }

    /**
     * Get expenses for user with optional date range filtering
     */
    public List<Expense> getExpensesForUserWithDateRange(String username, String startDateStr, String endDateStr) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_PREFIX + username));

        if (startDateStr != null && endDateStr != null) {
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);
            logger.debug("Fetching expenses for user {} between {} and {}", username, startDate, endDate);
            return expenseRepository.findByUserAndExpenseDateBetween(user, startDate, endDate);
        } else {
            logger.debug("Fetching all expenses for user {}", username);
            return expenseRepository.findByUser(user);
        }
    }

    /**
     * Get audit log for an expense
     */
    public List<AuditEntry> getAuditLog(Long expenseId) {
        return auditEntryRepository.findByExpenseIdOrderByCreatedAtAsc(expenseId);
    }

    /**
     * Mark an expense as reimbursed
     */
    @Transactional
    public Expense markAsReimbursed(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new NoSuchElementException(EXPENSE_NOT_FOUND_PREFIX + expenseId));

        if (expense.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new IllegalStateException(
                "Only approved expenses can be marked as reimbursed. Current status: " + expense.getApprovalStatus());
        }

        ApprovalStatus oldStatus = expense.getApprovalStatus();
        expense.setApprovalStatus(ApprovalStatus.REIMBURSED);
        expense.setReimbursedAt(java.time.LocalDateTime.now());
        Expense savedExpense = expenseRepository.save(expense);

        // Create audit entry for reimbursement
        createAuditEntryWithStatus(expense, expense.getUser(), "REIMBURSED",
            oldStatus.toString(), "REIMBURSED", "Expense marked as reimbursed");

        logger.info("Expense reimbursed: ID={}", expenseId);
        return savedExpense;
    }

    /**
     * Create an audit entry for expense actions
     */
    private void createAuditEntry(Expense expense, AppUser user, String action, String details) {
        AuditEntry auditEntry = new AuditEntry();
        auditEntry.setExpense(expense);
        auditEntry.setUser(user);
        auditEntry.setAction(action);
        auditEntry.setDetails(details);
        auditEntry.setPerformedBy(user != null ? user.getUsername() : "SYSTEM");
        auditEntryRepository.save(auditEntry);
    }

    /**
     * Create an audit entry with status transition
     */
    private void createAuditEntryWithStatus(Expense expense, AppUser user, String action,
                                           String oldStatus, String newStatus, String comment) {
        AuditEntry auditEntry = new AuditEntry();
        auditEntry.setExpense(expense);
        auditEntry.setUser(user);
        auditEntry.setAction(action);
        auditEntry.setOldStatus(oldStatus);
        auditEntry.setNewStatus(newStatus);
        auditEntry.setComment(comment);
        auditEntry.setPerformedBy(user != null ? user.getUsername() : "SYSTEM");
        auditEntryRepository.save(auditEntry);
    }

    /**
     * Get expenses with pagination, filtering, and sorting
     */
    public PageResponse<Expense> getExpensesWithFilters(String username, ExpenseFilterRequest filter) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_PREFIX + username));

        // Create sort
        Sort sort = Sort.by(
                "DESC".equalsIgnoreCase(filter.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                filter.getSortBy()
        );

        // Create pageable
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        // Apply filters
        Page<Expense> page = expenseRepository.findByFilters(
                user.getId(),
                filter.getStatus(),
                filter.getCategory(),
                filter.getMinAmount(),
                filter.getMaxAmount(),
                filter.getStartDate(),
                filter.getEndDate(),
                filter.getHasReceipt(),
                pageable
        );

        return convertToPageResponse(page);
    }

    /**
     * Get all expenses with pagination and filtering (for manager/finance)
     */
    public PageResponse<Expense> getAllExpensesWithFilters(ExpenseFilterRequest filter) {
        // Create sort
        Sort sort = Sort.by(
                "DESC".equalsIgnoreCase(filter.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                filter.getSortBy()
        );

        // Create pageable
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        // Apply filters (without userId restriction)
        Page<Expense> page = expenseRepository.findByFilters(
                null,  // No user restriction
                filter.getStatus(),
                filter.getCategory(),
                filter.getMinAmount(),
                filter.getMaxAmount(),
                filter.getStartDate(),
                filter.getEndDate(),
                filter.getHasReceipt(),
                pageable
        );

        return convertToPageResponse(page);
    }

    /**
     * Get expenses by status with pagination
     */
    public PageResponse<Expense> getExpensesByStatusWithPagination(
            ApprovalStatus status, int page, int size, String sortBy, String sortDirection) {
        Sort sort = Sort.by(
                "DESC".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy
        );
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Expense> expensePage = expenseRepository.findByApprovalStatus(status, pageable);
        return convertToPageResponse(expensePage);
    }

    /**
     * Convert Spring Page to custom PageResponse
     */
    private <T> PageResponse<T> convertToPageResponse(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
