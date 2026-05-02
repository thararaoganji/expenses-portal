package portal.expenses.repository;

import portal.expenses.entity.ExpenseApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseApprovalRepository extends JpaRepository<ExpenseApproval, Long> {

    List<ExpenseApproval> findByExpenseId(Long expenseId);

    List<ExpenseApproval> findByExpenseIdOrderByCreatedAtAsc(Long expenseId);
}
