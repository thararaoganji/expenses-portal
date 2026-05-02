package portal.expenses.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "policy_rules")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PolicyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_name", nullable = false, unique = true)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private PolicyRuleType ruleType;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "priority")
    private Integer priority = 0;

    // Condition fields
    @Enumerated(EnumType.STRING)
    @Column(name = "condition_category")
    private ExpenseCategory conditionCategory;

    @Column(name = "condition_amount_min")
    private BigDecimal conditionAmountMin;

    @Column(name = "condition_amount_max")
    private BigDecimal conditionAmountMax;

    @Column(name = "condition_age_days")
    private Integer conditionAgeDays;

    @Column(name = "condition_receipt_required")
    private Boolean conditionReceiptRequired;

    // Action fields
    @Enumerated(EnumType.STRING)
    @Column(name = "action_approval_status", nullable = false)
    private ApprovalStatus actionApprovalStatus;

    @Column(name = "action_reason", columnDefinition = "TEXT")
    private String actionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public PolicyRuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(PolicyRuleType ruleType) {
        this.ruleType = ruleType;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public ExpenseCategory getConditionCategory() {
        return conditionCategory;
    }

    public void setConditionCategory(ExpenseCategory conditionCategory) {
        this.conditionCategory = conditionCategory;
    }

    public BigDecimal getConditionAmountMin() {
        return conditionAmountMin;
    }

    public void setConditionAmountMin(BigDecimal conditionAmountMin) {
        this.conditionAmountMin = conditionAmountMin;
    }

    public BigDecimal getConditionAmountMax() {
        return conditionAmountMax;
    }

    public void setConditionAmountMax(BigDecimal conditionAmountMax) {
        this.conditionAmountMax = conditionAmountMax;
    }

    public Integer getConditionAgeDays() {
        return conditionAgeDays;
    }

    public void setConditionAgeDays(Integer conditionAgeDays) {
        this.conditionAgeDays = conditionAgeDays;
    }

    public Boolean getConditionReceiptRequired() {
        return conditionReceiptRequired;
    }

    public void setConditionReceiptRequired(Boolean conditionReceiptRequired) {
        this.conditionReceiptRequired = conditionReceiptRequired;
    }

    public ApprovalStatus getActionApprovalStatus() {
        return actionApprovalStatus;
    }

    public void setActionApprovalStatus(ApprovalStatus actionApprovalStatus) {
        this.actionApprovalStatus = actionApprovalStatus;
    }

    public String getActionReason() {
        return actionReason;
    }

    public void setActionReason(String actionReason) {
        this.actionReason = actionReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
