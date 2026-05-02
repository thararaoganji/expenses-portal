-- Update expenses table to add new fields (using VARCHAR for enum compatibility with JPA)
ALTER TABLE expenses
ADD COLUMN category VARCHAR(50) DEFAULT 'OTHER',
ADD COLUMN expense_date DATE NOT NULL DEFAULT CURRENT_DATE,
ADD COLUMN approval_status VARCHAR(50) DEFAULT 'PENDING',
ADD COLUMN has_receipt BOOLEAN DEFAULT true;

-- Create policy_rules table for configurable rules
CREATE TABLE policy_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_name VARCHAR(255) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    enabled BOOLEAN DEFAULT true,
    priority INT DEFAULT 0,

    -- Conditions (JSON-like flexible storage)
    condition_category VARCHAR(50),
    condition_amount_min NUMERIC(19, 2),
    condition_amount_max NUMERIC(19, 2),
    condition_age_days INT,
    condition_receipt_required BOOLEAN,

    -- Actions
    action_approval_status VARCHAR(50) NOT NULL,
    action_reason TEXT,

    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_rule_name UNIQUE(rule_name)
);

-- Create expense_approvals table to track approval workflow
CREATE TABLE expense_approvals (
    id BIGSERIAL PRIMARY KEY,
    expense_id BIGINT NOT NULL,
    approval_level VARCHAR(50) NOT NULL, -- MANAGER, FINANCE, AUTO
    status VARCHAR(50) NOT NULL,
    approver_id BIGINT, -- NULL for auto-approvals
    comments TEXT,
    applied_rule_id BIGINT, -- Which rule triggered this
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_expense
        FOREIGN KEY(expense_id)
        REFERENCES expenses(id) ON DELETE CASCADE,
    CONSTRAINT fk_approver
        FOREIGN KEY(approver_id)
        REFERENCES app_user(id),
    CONSTRAINT fk_policy_rule
        FOREIGN KEY(applied_rule_id)
        REFERENCES policy_rules(id)
);

-- Create indexes for performance
CREATE INDEX idx_expenses_status ON expenses(approval_status);
CREATE INDEX idx_expenses_category ON expenses(category);
CREATE INDEX idx_expense_approvals_expense_id ON expense_approvals(expense_id);
CREATE INDEX idx_policy_rules_enabled ON policy_rules(enabled, priority);

-- Insert default policy rules
INSERT INTO policy_rules (rule_name, rule_type, priority, condition_amount_min, condition_amount_max, action_approval_status, action_reason)
VALUES ('Auto-approve small expenses', 'AMOUNT_THRESHOLD', 1, 0, 1000, 'AUTO_APPROVED', 'Amount is less than or equal to 1000');

INSERT INTO policy_rules (rule_name, rule_type, priority, condition_category, condition_receipt_required, action_approval_status, action_reason)
VALUES ('Travel expenses require receipt', 'RECEIPT_REQUIRED', 2, 'TRAVEL', true, 'MANAGER_REVIEW', 'Travel expenses without receipt require manager review');

INSERT INTO policy_rules (rule_name, rule_type, priority, condition_amount_min, action_approval_status, action_reason)
VALUES ('High value expenses require dual approval', 'AMOUNT_THRESHOLD', 3, 5000, 'FINANCE_REVIEW', 'Amount exceeds 5000, requires manager and finance review');

INSERT INTO policy_rules (rule_name, rule_type, priority, condition_age_days, action_approval_status, action_reason)
VALUES ('Reject expenses older than 60 days', 'AGE_LIMIT', 4, 60, 'REJECTED', 'Expense date is older than 60 days');

INSERT INTO policy_rules (rule_name, rule_type, priority, condition_amount_min, condition_amount_max, action_approval_status, action_reason)
VALUES ('Medium expenses require manager review', 'AMOUNT_THRESHOLD', 5, 1000.01, 4999.99, 'MANAGER_REVIEW', 'Amount requires manager approval');
