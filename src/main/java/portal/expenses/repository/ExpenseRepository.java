package portal.expenses.repository;

import portal.expenses.entity.AppUser;
import portal.expenses.entity.ApprovalStatus;
import portal.expenses.entity.Expense;
import portal.expenses.entity.ExpenseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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
           "(:status IS NULL OR e.approvalStatus = :status) AND " +
           "(:category IS NULL OR e.category = :category) AND " +
           "(:minAmount IS NULL OR e.amount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR e.amount <= :maxAmount) AND " +
           "(:startDate IS NULL OR e.expenseDate >= :startDate) AND " +
           "(:endDate IS NULL OR e.expenseDate <= :endDate) AND " +
           "(:hasReceipt IS NULL OR e.hasReceipt = :hasReceipt)")
    @SuppressWarnings("java:S107") // Spring Data JPA requires binding each parameter individually for query method
    Page<Expense> findByFilters(
            @Param("userId") Long userId,
            @Param("status") ApprovalStatus status,
            @Param("category") ExpenseCategory category,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("hasReceipt") Boolean hasReceipt,
            Pageable pageable
    );
}