package com.example.workflow.model;

import java.io.Serializable;

public class RefundRequest implements Serializable {
    private String userId;
    private String refundReason;
    private double refundAmount;
    private String status;
    private String operatorComments;
    private boolean approvalRequired;
    private boolean hasPermission;
    private String approvalStatus;
    private String processInstanceId;

    public RefundRequest() {
    }

    public RefundRequest(String userId, String refundReason, double refundAmount) {
        this.userId = userId;
        this.refundReason = refundReason;
        this.refundAmount = refundAmount;
        this.status = "CREATED";
        this.approvalStatus = "CREATED";
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(double refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOperatorComments() {
        return operatorComments;
    }

    public void setOperatorComments(String operatorComments) {
        this.operatorComments = operatorComments;
    }

    public boolean isApprovalRequired() {
        return approvalRequired;
    }

    public void setApprovalRequired(boolean approvalRequired) {
        this.approvalRequired = approvalRequired;
    }

    public boolean isHasPermission() {
        return hasPermission;
    }

    public void setHasPermission(boolean hasPermission) {
        this.hasPermission = hasPermission;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public String toString() {
        return "RefundRequest{" +
                "userId='" + userId + '\'' +
                ", refundReason='" + refundReason + '\'' +
                ", refundAmount=" + refundAmount +
                ", status='" + status + '\'' +
                ", operatorComments='" + operatorComments + '\'' +
                ", approvalRequired=" + approvalRequired +
                ", hasPermission=" + hasPermission +
                ", approvalStatus='" + approvalStatus + '\'' +
                ", processInstanceId='" + processInstanceId + '\'' +
                '}';
    }
}