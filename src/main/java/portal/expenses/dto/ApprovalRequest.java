package portal.expenses.dto;

import portal.expenses.entity.ApprovalStatus;

public class ApprovalRequest {

    private ApprovalStatus status;
    private String comments;
    private String action;
    private Boolean approved;

    public ApprovalStatus getStatus() {
        // If status is already set, return it
        if (status != null) {
            return status;
        }

        // Try to derive from 'approved' boolean
        if (approved != null) {
            return approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED;
        }

        // Try to derive from 'action' string
        if (action != null) {
            try {
                return ApprovalStatus.valueOf(action.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Map common action names to ApprovalStatus
                switch (action.toLowerCase()) {
                    case "approve":
                        return ApprovalStatus.APPROVED;
                    case "reject":
                        return ApprovalStatus.REJECTED;
                    case "pending":
                        return ApprovalStatus.PENDING;
                    default:
                        return null;
                }
            }
        }

        return null;
    }

    public void setStatus(ApprovalStatus status) {
        this.status = status;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }
}
