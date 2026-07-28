package portal.expenses.controller;

import portal.expenses.dto.ApprovalRequest;
import portal.expenses.dto.PageResponse;
import portal.expenses.entity.ApprovalStatus;
import portal.expenses.entity.Expense;
import portal.expenses.service.ApprovalService;
import portal.expenses.service.ExpenseService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/manager")
@PreAuthorize("hasAuthority('ROLE_MANAGER')")
public class ManagerController {

    private static final Logger logger = LoggerFactory.getLogger(ManagerController.class);

    private final ExpenseService expenseService;
    private final ApprovalService approvalService;

    public ManagerController(ExpenseService expenseService, ApprovalService approvalService) {
        this.expenseService = expenseService;
        this.approvalService = approvalService;
    }

    /**
     * Get all expenses pending manager review
     */
    @GetMapping("/approval-queue")
    public ResponseEntity<List<Expense>> getApprovalQueue() {
        logger.debug("/approval-queue");
        logger.debug("Fetching approval queue...");
        List<Expense> expenses = expenseService.getExpensesByStatus(ApprovalStatus.MANAGER_REVIEW);
        return ResponseEntity.ok(expenses);
    }

    /**
     * Get all expenses pending manager review with pagination and sorting
     * Example: GET /manager/approval-queue/paginated?page=0&size=10&sortBy=createdAt&sortDirection=DESC
     */
    @GetMapping("/approval-queue/paginated")
    public ResponseEntity<PageResponse<Expense>> getApprovalQueuePaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        return ResponseEntity.ok(
                expenseService.getExpensesByStatusWithPagination(ApprovalStatus.MANAGER_REVIEW, page, size, sortBy, sortDirection)
        );
    }

    /**
     * Approve or reject an expense as a manager
     */
    @PostMapping("/{expenseId}/approve")
    public ResponseEntity<Expense> processApproval(
            @PathVariable Long expenseId,
            @Valid @RequestBody ApprovalRequest approvalRequest,
            Authentication authentication) {
        String username = authentication.getName();
        Expense approvedExpense = approvalService.processApproval(
                expenseId,
                username,
                approvalRequest.getStatus(),
                approvalRequest.getComments());
        return ResponseEntity.ok(approvedExpense);
    }
}
