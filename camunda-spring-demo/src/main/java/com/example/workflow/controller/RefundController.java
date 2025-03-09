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

        // Validate status input
        if (!"APPROVED".equals(status) && !"DENIED".equals(status)) {
            logger.error("Invalid status: {}", status);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            RefundRequest updatedRequest = refundService.updateRefundStatus(processInstanceId, status, comments);

            // Check if permission was denied by inspecting the response
            if (!updatedRequest.isHasPermission() && "DENIED".equals(updatedRequest.getStatus())) {
                logger.warn("Refund update denied due to lack of permission for process instance: {}", processInstanceId);
                return new ResponseEntity<>(updatedRequest, HttpStatus.FORBIDDEN); // 403 Forbidden
            }

            return new ResponseEntity<>(updatedRequest, HttpStatus.OK); // 200 OK for successful update

        } catch (IllegalArgumentException e) {
            logger.error("Error updating refund status: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND); // 404 Not Found for non-existent process or request
        } catch (Exception e) {
            logger.error("Unexpected error updating refund status: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); // 500 Internal Server Error for unexpected issues
        }
}

    @PostMapping("/complete/{processInstanceId}")
    public ResponseEntity<RefundRequest> completeRefund(@PathVariable String processInstanceId) {
        logger.info("Completing refund for process instance: {}", processInstanceId);

        try {
            RefundRequest completedRequest = refundService.completeRefund(processInstanceId);
            return new ResponseEntity<>(completedRequest, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            logger.error("Error completing refund: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}