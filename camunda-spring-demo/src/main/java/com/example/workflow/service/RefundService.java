package com.example.workflow.service;

import com.example.workflow.model.RefundRequest;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RefundService {

    private static final Logger logger = LoggerFactory.getLogger(RefundService.class);
    private static final String PROCESS_KEY = "Process_0fmtt81";

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    public RefundRequest initiateRefund(RefundRequest refundRequest) {
        logger.info("Initiating refund process for user: {}", refundRequest.getUserId());

        // To check this capability in workflow
        // CREATED
        if (refundRequest.getStatus() == null) {
            refundRequest.setStatus("CREATED");
            refundRequest.setApprovalStatus("CREATED");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("refundRequest", refundRequest);
        variables.put("approvalRequired", refundRequest.isApprovalRequired());
        variables.put("hasPermission", refundRequest.isHasPermission());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);
        refundRequest.setProcessInstanceId(processInstance.getId());
        runtimeService.setVariable(processInstance.getId(), "refundRequest", refundRequest);
        logger.info("Refund process started with instance ID: {}", processInstance.getId());

        return refundRequest;
    }

    public RefundRequest getRefundStatus(String processInstanceId) {
        logger.info("Fetching refund status for process instance: {}", processInstanceId);

        RefundRequest refundRequest = (RefundRequest) runtimeService.getVariable(processInstanceId, "refundRequest");
        if (refundRequest == null) {
            logger.error("No refund request found for process instance: {}", processInstanceId);
            throw new IllegalArgumentException("Refund request not found");
        }
        refundRequest.setProcessInstanceId(processInstanceId);
        return refundRequest;
    }

    public RefundRequest updateRefundStatus(String processInstanceId, String status, String comments) {
        logger.info("Updating refund status for process instance: {}, status: {}", processInstanceId, status);

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (processInstance == null) {
            logger.error("Process instance {} does not exist or has completed", processInstanceId);
            throw new IllegalArgumentException("Process instance not found or already completed");
        }

        RefundRequest refundRequest = (RefundRequest) runtimeService.getVariable(processInstanceId, "refundRequest");
        if (refundRequest == null) {
            logger.error("No refund request found for process instance: {}", processInstanceId);
            throw new IllegalArgumentException("Refund request not found");
        }

        // Check approvalRequired and hasPermission
        // refundRequest status to come from workflow
        // transition id function should be used in workflow.
        // this
//        processInstance.
        if (!refundRequest.isApprovalRequired()) {
            logger.warn("Approval not required for process instance: {}", processInstanceId);
            refundRequest.setStatus("NOT_REQUIRED");
            refundRequest.setApprovalStatus("NOT_REQUIRED");
            refundRequest.setOperatorComments("Operation not allowed: Approval is not required");
            runtimeService.setVariable(processInstanceId, "refundRequest", refundRequest);
            return refundRequest;
        }
        if (!refundRequest.isHasPermission()) {
            logger.warn("User {} lacks permission to update refund status for process instance: {}", refundRequest.getUserId(), processInstanceId);
            refundRequest.setStatus("DENIED");
            refundRequest.setApprovalStatus("DENIED");
            refundRequest.setOperatorComments("Permission denied: User lacks authorization to update refund status");
            runtimeService.setVariable(processInstanceId, "refundRequest", refundRequest);
            runtimeService.setVariable(processInstanceId, "isDenied", true);
            return refundRequest;
        }

        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (task == null) {
            logger.warn("No active task found for process instance: {}. Updating status without task completion.", processInstanceId);
            refundRequest.setStatus(status);
            refundRequest.setOperatorComments(comments);
            if ("APPROVED".equals(status)) {
                refundRequest.setApprovalStatus("APPROVED");
            } else if ("DENIED".equals(status)) {
                refundRequest.setApprovalStatus("DENIED");
                refundRequest.setStatus("DENIED");
                runtimeService.setVariable(processInstanceId, "isDenied", true);
            }
            refundRequest.setProcessInstanceId(processInstanceId);
            runtimeService.setVariable(processInstanceId, "refundRequest", refundRequest);
        } else {
            refundRequest.setStatus(status);
            refundRequest.setOperatorComments(comments);
            if ("APPROVED".equals(status)) {
                refundRequest.setApprovalStatus("APPROVED");
            } else if ("DENIED".equals(status)) {
                refundRequest.setApprovalStatus("DENIED");
                refundRequest.setStatus("DENIED");
                runtimeService.setVariable(processInstanceId, "isDenied", true);
            }
            refundRequest.setProcessInstanceId(processInstanceId);
            runtimeService.setVariable(processInstanceId, "refundRequest", refundRequest);

            try {
                taskService.complete(task.getId());
                logger.info("Task {} completed for process instance: {}", task.getId(), processInstanceId);
            } catch (Exception e) {
                logger.error("Failed to complete task {}: {}", task.getId(), e.getMessage());
                refundRequest.setOperatorComments(comments + " (Task completion failed: " + e.getMessage() + ")");
                runtimeService.setVariable(processInstanceId, "refundRequest", refundRequest);
            }
        }
        logger.info("Refund status updated to {}: {}", status, refundRequest);
        return refundRequest;
    }

    public RefundRequest completeRefund(String processInstanceId) {
        logger.info("Completing refund process for instance: {}", processInstanceId);

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        RefundRequest refundRequest = (RefundRequest) runtimeService.getVariable(processInstanceId, "refundRequest");
        if (refundRequest == null) {
            logger.error("No refund request found for process instance: {}", processInstanceId);
            throw new IllegalArgumentException("Refund request not found");
        }

        // Check approvalRequired and hasPermission
        if (!refundRequest.isApprovalRequired()) {
            logger.warn("Approval not required for process instance: {}", processInstanceId);
            refundRequest.setStatus("NOT_REQUIRED");
            refundRequest.setApprovalStatus("NOT_REQUIRED");
            refundRequest.setOperatorComments("Operation not allowed: Approval is not required");
            refundRequest.setProcessInstanceId(processInstanceId);
            runtimeService.setVariable(processInstanceId, "refundRequest", refundRequest);
            return refundRequest;
        }
        if (!refundRequest.isHasPermission()) {
            logger.warn("User {} lacks permission to complete refund for process instance: {}", refundRequest.getUserId(), processInstanceId);
            refundRequest.setStatus("DENIED");
            refundRequest.setApprovalStatus("DENIED");
            refundRequest.setOperatorComments("Permission denied: User lacks authorization to complete refund");
            refundRequest.setProcessInstanceId(processInstanceId);
            runtimeService.setVariable(processInstanceId, "refundRequest", refundRequest);
            runtimeService.setVariable(processInstanceId, "isDenied", true);
            return refundRequest;
        }

        if (processInstance == null || processInstance.isEnded()) {
            logger.info("Process already completed: {}", processInstanceId);
            refundRequest.setProcessInstanceId(processInstanceId);
            return refundRequest;
        }

        if (!"APPROVED".equals(refundRequest.getStatus())) {
            logger.warn("Cannot complete refund for process instance {}: Status is not APPROVED", processInstanceId);
            refundRequest.setOperatorComments("Refund cannot be completed: Current status is " + refundRequest.getStatus());
            runtimeService.setVariable(processInstanceId, "refundRequest", refundRequest);
            return refundRequest;
        }

        refundRequest.setStatus("COMPLETED");
        refundRequest.setProcessInstanceId(processInstanceId);
        runtimeService.setVariable(processInstanceId, "refundRequest", refundRequest);
        logger.info("Refund process completed: {}", refundRequest);

        return refundRequest;
    }

    // Delegate methods (unchanged)
    public void createRefundEntry(RefundRequest refundRequest) {
        refundRequest.setStatus("CREATED");
        refundRequest.setApprovalStatus("CREATED");
        logger.info("Created refund entry: {}", refundRequest);
    }

    public void initiateApprovalWorkflow(RefundRequest refundRequest) {
        refundRequest.setApprovalRequired(refundRequest.getRefundAmount() > 100.0);
        logger.info("Initiated approval workflow: {}", refundRequest);
    }

    public void setApprovalNotRequired(RefundRequest refundRequest) {
        refundRequest.setApprovalStatus("NOT_REQUIRED");
        refundRequest.setStatus("COMPLETED");
        logger.info("Set approval not required: {}", refundRequest);
    }

    public void setApprovalPending(RefundRequest refundRequest) {
        refundRequest.setApprovalStatus("APPROVAL_PENDING");
        refundRequest.setStatus("PENDING");
        logger.info("Set approval pending: {}", refundRequest);
    }

    public void setPermissionDenied(RefundRequest refundRequest) {
        refundRequest.setStatus("DENIED");
        refundRequest.setOperatorComments("Permission denied for refund");
        logger.info("Set permission denied: {}", refundRequest);
    }

    public void triggerNotifications(RefundRequest refundRequest) {
        logger.info("Triggered notifications for approvers: {}", refundRequest);
    }
}