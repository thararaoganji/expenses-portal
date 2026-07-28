package portal.expenses.service;

import portal.expenses.entity.ApprovalStatus;
import portal.expenses.entity.Expense;
import portal.expenses.entity.ExpenseApproval;
import portal.expenses.entity.PolicyRule;
import portal.expenses.repository.ExpenseApprovalRepository;
import portal.expenses.repository.PolicyRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class PolicyEngineService {

    private static final Logger logger = LoggerFactory.getLogger(PolicyEngineService.class);

    private final PolicyRuleRepository policyRuleRepository;
    private final ExpenseApprovalRepository expenseApprovalRepository;

    public PolicyEngineService(PolicyRuleRepository policyRuleRepository,
                               ExpenseApprovalRepository expenseApprovalRepository) {
        this.policyRuleRepository = policyRuleRepository;
        this.expenseApprovalRepository = expenseApprovalRepository;
    }

    /**
     * Evaluates all policy rules against an expense and determines the appropriate approval status.
     * Rules are evaluated in priority order, with the highest priority (lowest number) taking precedence.
     */
    @Transactional
    public ApprovalDecision evaluateExpense(Expense expense) {
        logger.debug("Evaluating expense: ID={}, amount={}, category={}",
                    expense.getId(), expense.getAmount(), expense.getCategory());

        List<PolicyRule> rules = policyRuleRepository.findAllEnabledOrderedByPriority();
        logger.debug("Evaluating {} policy rules", rules.size());

        ApprovalDecision decision = new ApprovalDecision();
        decision.setExpense(expense);

        // Track which rules match
        List<PolicyRule> matchedRules = new ArrayList<>();

        for (PolicyRule rule : rules) {
            if (evaluateRule(rule, expense)) {
                logger.debug("Policy rule matched: rule='{}', expenseId={}", rule.getRuleName(), expense.getId());
                matchedRules.add(rule);

                // For REJECTED status, immediately apply and stop evaluation
                if (rule.getActionApprovalStatus() == ApprovalStatus.REJECTED) {
                    decision.setFinalStatus(ApprovalStatus.REJECTED);
                    decision.setAppliedRule(rule);
                    decision.setReason(rule.getActionReason());
                    createApprovalRecord(expense, rule, ApprovalStatus.REJECTED, "AUTO");
                    logger.info("Expense rejected by policy: ID={}, rule='{}'", expense.getId(), rule.getRuleName());
                    return decision;
                }
            }
        }

        // Determine final status based on matched rules
        if (matchedRules.isEmpty()) {
            // No rules matched, default to pending
            decision.setFinalStatus(ApprovalStatus.PENDING);
            decision.setReason("No matching policy rules, manual review required");
        } else {
            // Apply the highest priority matched rule
            PolicyRule appliedRule = matchedRules.get(0);
            decision.setFinalStatus(appliedRule.getActionApprovalStatus());
            decision.setAppliedRule(appliedRule);
            decision.setReason(appliedRule.getActionReason());

            // Create approval records based on the status
            createApprovalRecordsForStatus(expense, appliedRule, appliedRule.getActionApprovalStatus());
        }

        logger.info("Policy evaluation complete: expenseId={}, status={}", expense.getId(), decision.getFinalStatus());
        return decision;
    }

    /**
     * Evaluates a single policy rule against an expense.
     */
    private boolean evaluateRule(PolicyRule rule, Expense expense) {
        return switch (rule.getRuleType()) {
            case AMOUNT_THRESHOLD, AUTO_APPROVE, MANAGER_APPROVAL, FINANCE_APPROVAL -> evaluateAmountThreshold(rule, expense);
            case CATEGORY_RULE -> evaluateCategoryRule(rule, expense);
            case AGE_LIMIT -> evaluateAgeLimit(rule, expense);
            case RECEIPT_REQUIRED -> evaluateReceiptRequired(rule, expense);
            default -> {
                logger.warn("Unknown rule type: {}", rule.getRuleType());
                yield false;
            }
        };
    }

    /**
     * Evaluates amount threshold rules.
     */
    private boolean evaluateAmountThreshold(PolicyRule rule, Expense expense) {
        boolean matches = true;

        if (rule.getConditionAmountMin() != null) {
            matches = expense.getAmount().compareTo(rule.getConditionAmountMin()) >= 0;
            logger.debug("Amount min check: expense={}, min={}, matches={}",
                        expense.getAmount(), rule.getConditionAmountMin(), matches);
        }

        if (matches && rule.getConditionAmountMax() != null) {
            matches = expense.getAmount().compareTo(rule.getConditionAmountMax()) <= 0;
            logger.debug("Amount max check: expense={}, max={}, matches={}",
                        expense.getAmount(), rule.getConditionAmountMax(), matches);
        }

        return matches;
    }

    /**
     * Evaluates category-based rules.
     */
    private boolean evaluateCategoryRule(PolicyRule rule, Expense expense) {
        return rule.getConditionCategory() != null
               && rule.getConditionCategory() == expense.getCategory();
    }

    /**
     * Evaluates age limit rules (expense date older than threshold).
     */
    private boolean evaluateAgeLimit(PolicyRule rule, Expense expense) {
        if (rule.getConditionAgeDays() == null) {
            return false;
        }

        long daysSinceExpense = ChronoUnit.DAYS.between(expense.getExpenseDate(), LocalDate.now());
        return daysSinceExpense > rule.getConditionAgeDays();
    }

    /**
     * Evaluates receipt requirement rules.
     */
    private boolean evaluateReceiptRequired(PolicyRule rule, Expense expense) {
        // Check if category matches
        if (rule.getConditionCategory() != null
            && rule.getConditionCategory() != expense.getCategory()) {
            return false;
        }

        // Check if receipt is required but missing
        return Boolean.TRUE.equals(rule.getConditionReceiptRequired())
               && Boolean.FALSE.equals(expense.getHasReceipt());
    }

    /**
     * Creates approval records based on the final status.
     */
    private void createApprovalRecordsForStatus(Expense expense, PolicyRule rule, ApprovalStatus status) {
        switch (status) {
            case AUTO_APPROVED -> createApprovalRecord(expense, rule, ApprovalStatus.AUTO_APPROVED, "AUTO");
            case MANAGER_REVIEW -> createApprovalRecord(expense, rule, ApprovalStatus.MANAGER_REVIEW, "MANAGER");
            case FINANCE_REVIEW -> {
                // Requires both manager and finance review
                createApprovalRecord(expense, rule, ApprovalStatus.MANAGER_REVIEW, "MANAGER");
                createApprovalRecord(expense, rule, ApprovalStatus.FINANCE_REVIEW, "FINANCE");
            }
            default -> {
                // PENDING or other statuses don't create approval records yet
            }
        }
    }

    /**
     * Creates an expense approval record.
     */
    private void createApprovalRecord(Expense expense, PolicyRule rule,
                                     ApprovalStatus status, String approvalLevel) {
        ExpenseApproval approval = new ExpenseApproval();
        approval.setExpense(expense);
        approval.setAppliedRule(rule);
        approval.setStatus(status);
        approval.setApprovalLevel(approvalLevel);
        approval.setComments(rule.getActionReason());
        expenseApprovalRepository.save(approval);
    }

    /**
     * Result of policy evaluation.
     */
    public static class ApprovalDecision {
        private Expense expense;
        private ApprovalStatus finalStatus;
        private PolicyRule appliedRule;
        private String reason;

        public Expense getExpense() {
            return expense;
        }

        public void setExpense(Expense expense) {
            this.expense = expense;
        }

        public ApprovalStatus getFinalStatus() {
            return finalStatus;
        }

        public void setFinalStatus(ApprovalStatus finalStatus) {
            this.finalStatus = finalStatus;
        }

        public PolicyRule getAppliedRule() {
            return appliedRule;
        }

        public void setAppliedRule(PolicyRule appliedRule) {
            this.appliedRule = appliedRule;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
