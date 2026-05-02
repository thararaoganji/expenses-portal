package portal.expenses.repository;

import portal.expenses.entity.AuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEntryRepository extends JpaRepository<AuditEntry, Long> {

    List<AuditEntry> findByExpenseIdOrderByCreatedAtDesc(Long expenseId);
    List<AuditEntry> findByExpenseIdOrderByCreatedAtAsc(Long expenseId);
}
