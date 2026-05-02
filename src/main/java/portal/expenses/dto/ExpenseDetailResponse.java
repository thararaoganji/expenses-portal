package portal.expenses.dto;

import portal.expenses.entity.Expense;

import java.util.List;

public class ExpenseDetailResponse {
    private Expense expense;
    private List<ExpenseApprovalResponse> approvals;

    public Expense getExpense() {
        return expense;
    }

    public void setExpense(Expense expense) {
        this.expense = expense;
    }

    public List<ExpenseApprovalResponse> getApprovals() {
        return approvals;
    }

    public void setApprovals(List<ExpenseApprovalResponse> approvals) {
        this.approvals = approvals;
    }
}
