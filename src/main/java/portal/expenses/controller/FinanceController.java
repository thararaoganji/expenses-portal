package portal.expenses.controller;

import portal.expenses.dto.PageResponse;
import portal.expenses.entity.ApprovalStatus;
import portal.expenses.entity.Expense;
import portal.expenses.service.ExpenseService;
import portal.expenses.service.FinanceService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/finance")
@PreAuthorize("hasAuthority('ROLE_FINANCE')")
public class FinanceController {

    private final ExpenseService expenseService;
    private final FinanceService financeService;

    public FinanceController(ExpenseService expenseService, FinanceService financeService) {
        this.expenseService = expenseService;
        this.financeService = financeService;
    }

    /**
     * Get all expenses that are approved and ready for reimbursement
     */
    @GetMapping("/reimbursement-queue")
    public ResponseEntity<List<Expense>> getReimbursementQueue() {
        List<Expense> expenses = expenseService.getExpensesByStatus(ApprovalStatus.APPROVED);
        return ResponseEntity.ok(expenses);
    }

    /**
     * Get reimbursement queue with pagination and sorting
     * Example: GET /finance/reimbursement-queue/paginated?page=0&size=10&sortBy=amount&sortDirection=DESC
     */
    @GetMapping("/reimbursement-queue/paginated")
    public ResponseEntity<PageResponse<Expense>> getReimbursementQueuePaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        return ResponseEntity.ok(
                expenseService.getExpensesByStatusWithPagination(ApprovalStatus.APPROVED, page, size, sortBy, sortDirection)
        );
    }

    /**
     * Get all reimbursed expenses
     */
    @GetMapping("/reimbursed")
    public ResponseEntity<List<Expense>> getReimbursedExpenses() {
        List<Expense> expenses = expenseService.getExpensesByStatus(ApprovalStatus.REIMBURSED);
        return ResponseEntity.ok(expenses);
    }

    /**
     * Get reimbursed expenses with pagination and sorting
     * Example: GET /finance/reimbursed/paginated?page=0&size=10&sortBy=reimbursedAt&sortDirection=DESC
     */
    @GetMapping("/reimbursed/paginated")
    public ResponseEntity<PageResponse<Expense>> getReimbursedExpensesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "reimbursedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        return ResponseEntity.ok(
                expenseService.getExpensesByStatusWithPagination(ApprovalStatus.REIMBURSED, page, size, sortBy, sortDirection)
        );
    }

    /**
     * Mark an expense as reimbursed
     */
    @PostMapping("/{expenseId}/reimburse")
    public ResponseEntity<Expense> markAsReimbursed(@PathVariable Long expenseId) {
        Expense expense = financeService.markAsReimbursed(expenseId);
        return ResponseEntity.ok(expense);
    }

    /**
     * Export reimbursed expenses to CSV
     */
    @GetMapping("/export/reimbursed")
    public ResponseEntity<Resource> exportReimbursedExpenses(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        byte[] csvData = financeService.exportReimbursedExpensesToCsv(startDate, endDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reimbursed_expenses.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new ByteArrayResource(csvData));
    }

    /**
     * Get all approved expenses (for finance review)
     */
    @GetMapping("/approved")
    public ResponseEntity<List<Expense>> getApprovedExpenses() {
        List<Expense> expenses = expenseService.getExpensesByStatus(ApprovalStatus.FINANCE_REVIEW);
        return ResponseEntity.ok(expenses);
    }
}
