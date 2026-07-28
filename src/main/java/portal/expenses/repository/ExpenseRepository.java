package portal.expenses.repository;

import portal.expenses.entity.AppUser;
import portal.expenses.entity.ApprovalStatus;
import portal.expenses.entity.Expense;
import portal.expenses.dto.ExpenseFilterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {
    // Existing methods
    List<Expense> findByUserId(Long userId);
    List<Expense> findByUser(AppUser user);
    List<Expense> findByUserAndExpenseDateBetween(AppUser user, LocalDate startDate, LocalDate endDate);
    List<Expense> findByApprovalStatus(ApprovalStatus status);
    List<Expense> findByApprovalStatusAndExpenseDateBetween(ApprovalStatus status, LocalDate startDate, LocalDate endDate);

    // Pageable methods
    Page<Expense> findByUser(AppUser user, Pageable pageable);
    Page<Expense> findByApprovalStatus(ApprovalStatus status, Pageable pageable);

    @Query("SELECT e FROM Expense e WHERE " +
           "(:userId IS NULL OR e.user.id = :userId) AND " +
           "(:#{#filter.status} IS NULL OR e.approvalStatus = :#{#filter.status}) AND " +
           "(:#{#filter.category} IS NULL OR e.category = :#{#filter.category}) AND " +
           "(:#{#filter.minAmount} IS NULL OR e.amount >= :#{#filter.minAmount}) AND " +
           "(:#{#filter.maxAmount} IS NULL OR e.amount <= :#{#filter.maxAmount}) AND " +
           "(:#{#filter.startDate} IS NULL OR e.expenseDate >= :#{#filter.startDate}) AND " +
           "(:#{#filter.endDate} IS NULL OR e.expenseDate <= :#{#filter.endDate}) AND " +
           "(:#{#filter.hasReceipt} IS NULL OR e.hasReceipt = :#{#filter.hasReceipt})")
    Page<Expense> findByFilters(
            @Param("userId") Long userId,
            @Param("filter") ExpenseFilterRequest filter,
            Pageable pageable
    );
}