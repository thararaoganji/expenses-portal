package portal.expenses.repository;

import portal.expenses.entity.PolicyRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyRuleRepository extends JpaRepository<PolicyRule, Long> {

    @Query("SELECT pr FROM PolicyRule pr WHERE pr.enabled = true ORDER BY pr.priority ASC")
    List<PolicyRule> findAllEnabledOrderedByPriority();

    List<PolicyRule> findByEnabledTrue();
}
