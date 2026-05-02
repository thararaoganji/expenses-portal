package portal.expenses.service;

import portal.expenses.entity.ApprovalStatus;
import portal.expenses.entity.Expense;
import portal.expenses.repository.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class FinanceService {

    private static final Logger logger = LoggerFactory.getLogger(FinanceService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ExpenseRepository expenseRepository;

    public FinanceService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    /**
     * Mark an expense as reimbursed
     */
    @Transactional
    public Expense markAsReimbursed(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found with ID: " + expenseId));

        if (expense.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new IllegalStateException(
                "Only approved expenses can be marked as reimbursed. Current status: " + expense.getApprovalStatus());
        }

        expense.setApprovalStatus(ApprovalStatus.REIMBURSED);
        Expense savedExpense = expenseRepository.save(expense);

        logger.info("Expense {} marked as reimbursed", expenseId);
        return savedExpense;
    }

    /**
     * Export reimbursed expenses to CSV format
     */
    public byte[] exportReimbursedExpensesToCsv(String startDateStr, String endDateStr) {
        List<Expense> expenses;

        if (startDateStr != null && endDateStr != null) {
            LocalDate startDate = LocalDate.parse(startDateStr, DATE_FORMATTER);
            LocalDate endDate = LocalDate.parse(endDateStr, DATE_FORMATTER);
            expenses = expenseRepository.findByApprovalStatusAndExpenseDateBetween(
                ApprovalStatus.REIMBURSED, startDate, endDate);
        } else {
            expenses = expenseRepository.findByApprovalStatus(ApprovalStatus.REIMBURSED);
        }

        StringBuilder csv = new StringBuilder();
        
        // CSV Header
        csv.append("Expense ID,Employee,Description,Amount,Category,Expense Date,Submitted Date,Status,Has Receipt\n");

        // CSV Rows
        for (Expense expense : expenses) {
            csv.append(expense.getId()).append(",")
               .append(escapeCsvField(expense.getUser().getUsername())).append(",")
               .append(escapeCsvField(expense.getDescription())).append(",")
               .append(expense.getAmount()).append(",")
               .append(expense.getCategory()).append(",")
               .append(expense.getExpenseDate()).append(",")
               .append(expense.getCreatedAt()).append(",")
               .append(expense.getApprovalStatus()).append(",")
               .append(expense.getHasReceipt()).append("\n");
        }

        logger.info("Exported {} reimbursed expenses to CSV", expenses.size());
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Escape CSV fields that contain commas, quotes, or newlines
     */
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
