-- V5: Schema enhancements for expenses, policy flags, audit, and timestamps
-- This migration consolidates V5, V6, and V7 for cleaner deployment

-- Make s3_object_key column nullable since receipts are optional
ALTER TABLE expenses ALTER COLUMN s3_object_key DROP NOT NULL;

-- Add policy_flags column to expenses table
ALTER TABLE expenses
ADD COLUMN policy_flags TEXT;

-- Add timestamp fields to expenses table for tracking status transitions
ALTER TABLE expenses ADD COLUMN submitted_at TIMESTAMP;
ALTER TABLE expenses ADD COLUMN approved_at TIMESTAMP;
ALTER TABLE expenses ADD COLUMN rejected_at TIMESTAMP;
ALTER TABLE expenses ADD COLUMN reimbursed_at TIMESTAMP;

-- Create audit_entries table
CREATE TABLE audit_entries (
    id BIGSERIAL PRIMARY KEY,
    expense_id BIGINT NOT NULL,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    details TEXT,
    old_status VARCHAR(50),
    new_status VARCHAR(50),
    comment TEXT,
    performed_by VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_expense
        FOREIGN KEY(expense_id)
        REFERENCES expenses(id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_user
        FOREIGN KEY(user_id)
        REFERENCES app_user(id)
);

-- Create indexes for performance
CREATE INDEX idx_audit_entries_expense_id ON audit_entries(expense_id);
CREATE INDEX idx_audit_entries_created_at ON audit_entries(created_at DESC);
