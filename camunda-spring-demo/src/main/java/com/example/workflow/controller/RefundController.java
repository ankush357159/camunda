package com.example.workflow.controller;

import com.example.workflow.model.RefundRequest;
import com.example.workflow.service.RefundService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/refund")
public class RefundController {

    private static final Logger logger = LoggerFactory.getLogger(RefundController.class);

    @Autowired
    private RefundService refundService;

    @PostMapping("/initiate")
    public ResponseEntity<RefundRequest> initiateRefund(@RequestBody RefundRequest refundRequest) {
        logger.info("Received refund request: {}", refundRequest);

        if (refundRequest.getUserId() == null || refundRequest.getRefundReason() == null || refundRequest.getRefundAmount() <= 0) {
            logger.error("Invalid refund request data");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        RefundRequest initiatedRequest = refundService.initiateRefund(refundRequest);
        return new ResponseEntity<>(initiatedRequest, HttpStatus.CREATED);
    }

    @GetMapping("/status/{processInstanceId}")
    public ResponseEntity<RefundRequest> getRefundStatus(@PathVariable String processInstanceId) {
        logger.info("Fetching status for process instance: {}", processInstanceId);

        try {
            RefundRequest refundRequest = refundService.getRefundStatus(processInstanceId);
            return new ResponseEntity<>(refundRequest, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            logger.error("Error fetching refund status: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/update/{processInstanceId}")
    public ResponseEntity<RefundRequest> updateRefundStatus(
            @PathVariable String processInstanceId,
            @RequestParam String status,
            @RequestParam(required = false) String comments) {
        logger.info("Updating refund status for process instance: {}, status: {}", processInstanceId, status);

        if (!"APPROVED".equals(status) && !"DENIED".equals(status)) {
            logger.error("Invalid status: {}", status);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            RefundRequest updatedRequest = refundService.updateRefundStatus(processInstanceId, status, comments);
            if (!updatedRequest.isHasPermission() || !updatedRequest.isApprovalRequired()) {
                logger.warn("Refund update denied for process instance: {} due to lack of permission or approval not required", processInstanceId);
                return new ResponseEntity<>(updatedRequest, HttpStatus.FORBIDDEN);
            }
            return new ResponseEntity<>(updatedRequest, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            logger.error("Error updating refund status: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Unexpected error updating refund status: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/complete/{processInstanceId}")
    public ResponseEntity<RefundRequest> completeRefund(@PathVariable String processInstanceId) {
        logger.info("Completing refund for process instance: {}", processInstanceId);

        try {
            RefundRequest completedRequest = refundService.completeRefund(processInstanceId);
            if (!completedRequest.isHasPermission() || !completedRequest.isApprovalRequired()) {
                logger.warn("Refund completion denied for process instance: {} due to lack of permission or approval not required", processInstanceId);
                return new ResponseEntity<>(completedRequest, HttpStatus.FORBIDDEN);
            }
            if (!"COMPLETED".equals(completedRequest.getStatus())) {
                logger.warn("Refund not completed for process instance: {} - status is {}", processInstanceId, completedRequest.getStatus());
                return new ResponseEntity<>(completedRequest, HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(completedRequest, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            logger.error("Error completing refund: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Unexpected error completing refund: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}