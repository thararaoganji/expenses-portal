package portal.expenses.service;

import portal.expenses.repository.AuditEntryRepository;
import portal.expenses.repository.ExpenseApprovalRepository;
import portal.expenses.repository.ExpenseRepository;
import portal.expenses.repository.UserRepository;
import portal.expenses.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ApprovalService {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalService.class);
    private static final String USER_NOT_FOUND_PREFIX = "User not found: ";
    private static final String EXPENSE_NOT_FOUND_PREFIX = "Expense not found with ID: ";

    private final ExpenseRepository expenseRepository;
    private final ExpenseApprovalRepository expenseApprovalRepository;
    private final UserRepository userRepository;
    private final AuditEntryRepository auditEntryRepository;

    public ApprovalService(ExpenseRepository expenseRepository,
                          ExpenseApprovalRepository expenseApprovalRepository,
                          UserRepository userRepository,
                          AuditEntryRepository auditEntryRepository) {
        this.expenseRepository = expenseRepository;
        this.expenseApprovalRepository = expenseApprovalRepository;
        this.userRepository = userRepository;
        this.auditEntryRepository = auditEntryRepository;
    }

    /**
     * Process an approval decision for an expense.
     */
    @Transactional
    public Expense processApproval(Long expenseId, String username, ApprovalStatus newStatus, String comments) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new NoSuchElementException(EXPENSE_NOT_FOUND_PREFIX + expenseId));

        AppUser approver = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException(USER_NOT_FOUND_PREFIX + username));

        // Validate state transition
        validateApprovalTransition(expense, newStatus);

        // Get pending approvals
        List<ExpenseApproval> approvals = expenseApprovalRepository.findByExpenseIdOrderByCreatedAtAsc(expenseId);

        // Determine approval level based on current status
        String approvalLevel = determineApprovalLevel(expense.getApprovalStatus());

        // Update the pending approval record
        boolean approvalUpdated = false;
        for (ExpenseApproval approval : approvals) {
            if (approval.getStatus() == expense.getApprovalStatus() && approval.getApprovalLevel().equals(approvalLevel)) {
                approval.setStatus(newStatus);
                approval.setApprover(approver);
                if (comments != null) {
                    approval.setComments(comments);
                }
                expenseApprovalRepository.save(approval);
                approvalUpdated = true;
                break;
            }
        }

        // If no existing approval record was updated, create a new one
        if (!approvalUpdated) {
            ExpenseApproval newApproval = new ExpenseApproval();
            newApproval.setExpense(expense);
            newApproval.setApprovalLevel(approvalLevel);
            newApproval.setStatus(newStatus);
            newApproval.setApprover(approver);
            newApproval.setComments(comments);
            expenseApprovalRepository.save(newApproval);
        }

        // Update expense status and timestamp
        ApprovalStatus oldStatus = expense.getApprovalStatus();
        expense.setApprovalStatus(newStatus);

        // Set appropriate timestamp based on status
        if (newStatus == ApprovalStatus.APPROVED) {
            expense.setApprovedAt(java.time.LocalDateTime.now());
        } else if (newStatus == ApprovalStatus.REJECTED) {
            expense.setRejectedAt(java.time.LocalDateTime.now());
        }

        expenseRepository.save(expense);

        // Create audit entry for status transition
        String actionName = determineActionName(approvalLevel, newStatus);
        createAuditEntry(expense, approver, actionName, oldStatus.toString(), newStatus.toString(), comments);

        logger.info("Approval processed: expenseId={}, user={}, status={}", expenseId, username, newStatus);

        return expense;
    }

    /**
     * Get all approval records for an expense.
     */
    public List<ExpenseApproval> getApprovalHistory(Long expenseId) {
        return expenseApprovalRepository.findByExpenseIdOrderByCreatedAtAsc(expenseId);
    }

    /**
     * Validate if the approval transition is allowed.
     */
    private void validateApprovalTransition(Expense expense, ApprovalStatus newStatus) {
        ApprovalStatus currentStatus = expense.getApprovalStatus();

        // Define valid transitions
        switch (currentStatus) {
            case PENDING:
                // Can transition to any status
                break;
            case MANAGER_REVIEW:
                if (newStatus != ApprovalStatus.APPROVED && newStatus != ApprovalStatus.REJECTED
                    && newStatus != ApprovalStatus.FINANCE_REVIEW) {
                    throw new IllegalStateException(
                        "Invalid transition from MANAGER_REVIEW to " + newStatus);
                }
                break;
            case FINANCE_REVIEW:
                if (newStatus != ApprovalStatus.APPROVED && newStatus != ApprovalStatus.REJECTED) {
                    throw new IllegalStateException(
                        "Invalid transition from FINANCE_REVIEW to " + newStatus);
                }
                break;
            case AUTO_APPROVED, APPROVED, REJECTED:
                throw new IllegalStateException(
                    "Cannot change status of expense in " + currentStatus + " state");
            default:
                throw new IllegalStateException("Unknown approval status: " + currentStatus);
        }
    }

    /**
     * Determine the approval level based on current status.
     */
    private String determineApprovalLevel(ApprovalStatus currentStatus) {
        if (currentStatus == ApprovalStatus.MANAGER_REVIEW) {
            return "MANAGER";
        } else if (currentStatus == ApprovalStatus.FINANCE_REVIEW) {
            return "FINANCE";
        } else {
            return "GENERAL";
        }
    }

    /**
     * Determine the action name based on approval level and status
     */
    private String determineActionName(String approvalLevel, ApprovalStatus status) {
        if (status == ApprovalStatus.APPROVED) {
            return approvalLevel + "_APPROVED";
        } else if (status == ApprovalStatus.REJECTED) {
            return approvalLevel + "_REJECTED";
        } else {
            return "STATUS_CHANGE";
        }
    }

    /**
     * Create an audit entry for approval actions
     */
    private void createAuditEntry(Expense expense, AppUser user, String action,
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
}
