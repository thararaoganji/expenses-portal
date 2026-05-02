package portal.expenses.controller;

import portal.expenses.dto.AuditLogResponse;
import portal.expenses.entity.ApprovalStatus;
import portal.expenses.entity.AuditEntry;
import jakarta.validation.Valid;
import portal.expenses.dto.ApprovalRequest;
import portal.expenses.dto.ExpenseApprovalResponse;
import portal.expenses.dto.ExpenseDetailResponse;
import portal.expenses.dto.ExpenseFilterRequest;
import portal.expenses.dto.PageResponse;
import portal.expenses.entity.Expense;
import portal.expenses.dto.ExpenseRequest;
import portal.expenses.entity.ExpenseApproval;
import portal.expenses.service.ApprovalService;
import portal.expenses.service.ExpenseService;
import portal.expenses.service.LocalStorageService;
import portal.expenses.service.S3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseController.class);

    @Value("${storage.mode}")
    private String storageMode;

    private final ExpenseService expenseService;
    private final S3Service s3Service;
    private final LocalStorageService localStorageService;
    private final ApprovalService approvalService;

    public ExpenseController(ExpenseService expenseService, S3Service s3Service, 
                           LocalStorageService localStorageService, ApprovalService approvalService) {
        this.expenseService = expenseService;
        this.s3Service = s3Service;
        this.localStorageService = localStorageService;
        this.approvalService = approvalService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Expense> createExpense(
            @Valid @RequestPart("expense") ExpenseRequest expenseRequest,
            @RequestPart(value = "receipt", required = false) MultipartFile receipt,
            @RequestParam(value = "draft", required = false, defaultValue = "false") boolean draft,
            Authentication authentication) throws IOException {
        String username = authentication.getName();
        logger.info("Creating expense for user: {}, amount: {}, category: {}, draft: {}",
                   username, expenseRequest.getAmount(), expenseRequest.getCategory(), draft);

        Expense createdExpense = expenseService.createExpense(
                expenseRequest.getDescription(),
                expenseRequest.getAmount(),
                receipt,
                username,
                expenseRequest.getCategory(),
                expenseRequest.getExpenseDate(),
                draft);

        logger.info("Expense created: ID={}, status={}", createdExpense.getId(), createdExpense.getApprovalStatus());
        return ResponseEntity.ok(createdExpense);
    }

    @GetMapping("/my-expenses")
    public ResponseEntity<List<Expense>> getMyExpenses(Authentication authentication) {
        String username = authentication.getName();
        logger.debug("Fetching expenses for user: {}", username);
        List<Expense> expenses = expenseService.getExpensesForCurrentUser(username);
        logger.debug("Retrieved {} expenses for user: {}", expenses.size(), username);
        return ResponseEntity.ok(expenses);
    }

    /**
     * Get my expenses with pagination, filtering, and sorting
     */
    @PostMapping("/my-expenses/search")
    public ResponseEntity<PageResponse<Expense>> getMyExpensesWithFilters(
            @RequestBody ExpenseFilterRequest filter,
            Authentication authentication) {
        String username = authentication.getName();
        logger.info("Fetching expenses with filters for user: {} - page: {}, size: {}, sort: {} {}",
                   username, filter.getPage(), filter.getSize(), filter.getSortBy(), filter.getSortDirection());
        PageResponse<Expense> response = expenseService.getExpensesWithFilters(username, filter);
        logger.info("Retrieved {} expenses (page {}/{}) for user: {}",
                   response.getContent().size(), response.getPageNumber() + 1, response.getTotalPages(), username);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending-approvals")
    public ResponseEntity<List<Expense>> getPendingApprovals(Authentication authentication) {
        String username = authentication.getName();
        logger.debug("Fetching pending approvals for user: {}", username);
        List<Expense> expenses = expenseService.getExpensesByStatus(ApprovalStatus.MANAGER_REVIEW);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/approved")
    public ResponseEntity<List<Expense>> getApprovedExpenses() {
        logger.debug("Fetching approved expenses");
        List<Expense> expenses = expenseService.getExpensesByStatus(ApprovalStatus.APPROVED);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/manager-queue")
    public ResponseEntity<List<Expense>> getManagerQueue() {
        logger.debug("Fetching manager approval queue");
        List<Expense> expenses = expenseService.getExpensesByStatus(ApprovalStatus.MANAGER_REVIEW);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/finance-queue")
    public ResponseEntity<List<Expense>> getFinanceQueue() {
        logger.debug("Fetching finance approval queue");
        List<Expense> expenses = expenseService.getExpensesByStatus(ApprovalStatus.FINANCE_REVIEW);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/reimbursed")
    public ResponseEntity<List<Expense>> getReimbursedExpenses() {
        logger.debug("Fetching reimbursed expenses");
        List<Expense> expenses = expenseService.getExpensesByStatus(ApprovalStatus.REIMBURSED);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<Expense> getExpense(@PathVariable Long expenseId) {
        logger.debug("Fetching expense: ID={}", expenseId);
        Expense expense = expenseService.getExpenseById(expenseId);
        return ResponseEntity.ok(expense);
    }

    /**
     * Get expense details with approval history and policy information
     */
    @GetMapping("/{expenseId}/details")
    public ResponseEntity<ExpenseDetailResponse> getExpenseDetails(@PathVariable Long expenseId) {
        Expense expense = expenseService.getExpenseById(expenseId);
        List<ExpenseApproval> approvals = approvalService.getApprovalHistory(expenseId);

        ExpenseDetailResponse response = new ExpenseDetailResponse();
        response.setExpense(expense);
        response.setApprovals(approvals.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    /**
     * Submit an expense (change status from DRAFT to SUBMITTED)
     */
    @PostMapping("/{expenseId}/submit")
    public ResponseEntity<Expense> submitExpense(@PathVariable Long expenseId, Authentication authentication) {
        String username = authentication.getName();
        logger.info("Submitting expense: ID={}, user={}", expenseId, username);
        Expense submittedExpense = expenseService.submitExpense(expenseId, username);
        logger.info("Expense submitted: ID={}, status={}", expenseId, submittedExpense.getApprovalStatus());
        return ResponseEntity.ok(submittedExpense);
    }

    /**
     * Update a draft expense (only DRAFT expenses can be updated)
     */
    @PutMapping("/{expenseId}")
    public ResponseEntity<Expense> updateExpense(
            @PathVariable Long expenseId,
            @Valid @RequestPart("expense") ExpenseRequest expenseRequest,
            @RequestPart(value = "receipt", required = false) MultipartFile receipt,
            Authentication authentication) throws IOException {
        String username = authentication.getName();
        logger.info("Updating expense: ID={}, user={}", expenseId, username);

        Expense updatedExpense = expenseService.updateExpense(
                expenseId,
                expenseRequest.getDescription(),
                expenseRequest.getAmount(),
                receipt,
                username,
                expenseRequest.getCategory(),
                expenseRequest.getExpenseDate());

        logger.info("update expense completed");

        logger.info("Expense updated: ID={}, status={}", expenseId, updatedExpense.getApprovalStatus());
        return ResponseEntity.ok(updatedExpense);
    }

    @GetMapping("/{expenseId}/receipt")
    public ResponseEntity<Resource> getReceipt(@PathVariable Long expenseId) {
        Expense expense = expenseService.getExpenseById(expenseId);
        if (expense.getS3ObjectKey() == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Download from the configured storage
        byte[] receiptBytes;
        if ("s3".equalsIgnoreCase(storageMode)) {
            receiptBytes = s3Service.downloadFile(expense.getS3ObjectKey());
        } else {
            receiptBytes = localStorageService.downloadFile(expense.getS3ObjectKey());
        }
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + expense.getS3ObjectKey() + "\"")
                .body(new ByteArrayResource(receiptBytes));
    }

    @PostMapping("/{expenseId}/approve")
    public ResponseEntity<Expense> approveExpense(
            @PathVariable Long expenseId,
            @RequestBody ApprovalRequest approvalRequest,
            Authentication authentication) {
        String username = authentication.getName();

        // Get the approval status (supports multiple field formats)
        ApprovalStatus approvalStatus = approvalRequest.getStatus();

        if (approvalStatus == null) {
            logger.warn("Missing approval status in request: expenseId={}", expenseId);
            throw new IllegalArgumentException("Approval status must be provided (use 'status', 'action', or 'approved' field)");
        }

        logger.info("Processing approval: expenseId={}, user={}, status={}", expenseId, username, approvalStatus);
        Expense approvedExpense = approvalService.processApproval(
                expenseId,
                username,
                approvalStatus,
                approvalRequest.getComments());
        logger.info("Approval completed: expenseId={}, finalStatus={}", expenseId, approvedExpense.getApprovalStatus());
        return ResponseEntity.ok(approvedExpense);
    }

    @GetMapping("/{expenseId}/approvals")
    public ResponseEntity<List<ExpenseApprovalResponse>> getApprovalHistory(@PathVariable Long expenseId) {
        List<ExpenseApproval> approvals = approvalService.getApprovalHistory(expenseId);
        List<ExpenseApprovalResponse> responses = approvals.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Get audit log for an expense
     */
    @GetMapping("/{expenseId}/audit-log")
    public ResponseEntity<List<AuditLogResponse>> getAuditLog(@PathVariable Long expenseId) {
        logger.debug("Fetching audit log: expenseId={}", expenseId);
        List<AuditEntry> auditLog = expenseService.getAuditLog(expenseId);

        List<AuditLogResponse> response = auditLog.stream()
                .map(this::mapToAuditResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    private AuditLogResponse mapToAuditResponse(AuditEntry audit) {
        AuditLogResponse response = new AuditLogResponse();
        response.setId(audit.getId());
        response.setAction(audit.getAction());
        response.setOldStatus(audit.getOldStatus());
        response.setNewStatus(audit.getNewStatus());
        response.setComment(audit.getComment());
        response.setPerformedBy(audit.getPerformedBy());
        response.setCreatedAt(audit.getCreatedAt());
        return response;
    }

    /**
     * Mark an expense as reimbursed (delegates to finance service)
     */
    @PostMapping("/{expenseId}/reimburse")
    public ResponseEntity<Expense> markAsReimbursed(@PathVariable Long expenseId, Authentication authentication) {
        String username = authentication.getName();
        logger.info("Marking expense as reimbursed: expenseId={}, user={}", expenseId, username);
        Expense expense = expenseService.markAsReimbursed(expenseId);
        return ResponseEntity.ok(expense);
    }

    /**
     * Export expenses to CSV with optional date range filtering
     */
    @GetMapping("/export/csv")
    public ResponseEntity<Resource> exportExpensesToCsv(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication authentication) {
        String username = authentication.getName();
        logger.info("Exporting expenses to CSV: user={}, dateRange=[{} to {}]", username, startDate, endDate);

        List<Expense> expenses = expenseService.getExpensesForUserWithDateRange(username, startDate, endDate);
        byte[] csvData = generateCsv(expenses);

        logger.info("CSV export completed: {} expenses, {} bytes", expenses.size(), csvData.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"expenses.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new ByteArrayResource(csvData));
    }

    private byte[] generateCsv(List<Expense> expenses) {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Description,Amount,Category,Expense Date,Status,Has Receipt,Created At\n");

        for (Expense expense : expenses) {
            csv.append(expense.getId()).append(",");
            csv.append("\"").append(expense.getDescription().replace("\"", "\"\"")).append("\",");
            csv.append(expense.getAmount()).append(",");
            csv.append(expense.getCategory()).append(",");
            csv.append(expense.getExpenseDate()).append(",");
            csv.append(expense.getApprovalStatus()).append(",");
            csv.append(expense.getHasReceipt()).append(",");
            csv.append(expense.getCreatedAt()).append("\n");
        }

        return csv.toString().getBytes();
    }

    private ExpenseApprovalResponse mapToResponse(ExpenseApproval approval) {
        ExpenseApprovalResponse response = new ExpenseApprovalResponse();
        response.setId(approval.getId());
        response.setExpenseId(approval.getExpense().getId());
        response.setApprovalLevel(approval.getApprovalLevel());
        response.setStatus(approval.getStatus());
        response.setComments(approval.getComments());
        response.setCreatedAt(approval.getCreatedAt());
        if (approval.getApprover() != null) {
            response.setApproverUsername(approval.getApprover().getUsername());
        }
        if (approval.getAppliedRule() != null) {
            response.setAppliedRuleName(approval.getAppliedRule().getRuleName());
        }
        return response;
    }
}