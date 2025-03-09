package com.example.workflow;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/refund")
public class RefundController {

    private static final Logger logger = Logger.getLogger(RefundController.class.getName());
    private List<RefundRequest> refundRequests = new ArrayList<>();

    // New refund request
    @PostMapping("/request")
    public ResponseEntity<RefundRequest> createRefundRequest(@RequestBody RefundRequest refundRequest) {
        logger.info("-------Refund Request Creator Initialized-------");

        RefundRequest newRequest = new RefundRequest();
        newRequest.setUserId(refundRequest.getUserId());
        newRequest.setRefundReason(refundRequest.getRefundReason());
        newRequest.setRefundAmount(refundRequest.getRefundAmount());
        newRequest.setStatus("PENDING");
        newRequest.setApprovalRequired(refundRequest.isApprovalRequired());
        newRequest.setHasPermission(refundRequest.isHasPermission());
        newRequest.setApprovalStatus("AWAITING");

        refundRequests.add(newRequest);
        logger.info("Refund Request created --> " + newRequest.toString());

        return new ResponseEntity<>(newRequest, HttpStatus.CREATED);
    }


    @GetMapping("/requests")
    public ResponseEntity<List<RefundRequest>> getAllRefundRequests() {
        logger.info("-------Retrieving all refund requests-------");
        logger.info("Total requests found: " + refundRequests.size());
        return new ResponseEntity<>(refundRequests, HttpStatus.OK);
    }


    @GetMapping("/request/{userId}")
    public ResponseEntity<RefundRequest> getRefundRequestByUserId(@PathVariable String userId) {
        logger.info("-------Retrieving refund request for userId: " + userId + "-------");

        return refundRequests.stream()
                .filter(request -> request.getUserId().equals(userId))
                .findFirst()
                .map(request -> {
                    logger.info("Refund request found: " + request.toString());
                    return new ResponseEntity<>(request, HttpStatus.OK);
                })
                .orElseGet(() -> {
                    logger.info("No refund request found for userId: " + userId);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                });
    }


    @PutMapping("/request/{userId}")
    public ResponseEntity<RefundRequest> updateRefundRequest(
            @PathVariable String userId,
            @RequestBody RefundRequest updatedRequest) {
        logger.info("-------Updating refund request for userId: " + userId + "-------");

        for (RefundRequest request : refundRequests) {
            if (request.getUserId().equals(userId)) {
                request.setRefundReason(updatedRequest.getRefundReason());
                request.setRefundAmount(updatedRequest.getRefundAmount());
                request.setStatus(updatedRequest.getStatus());
                request.setOperatorComments(updatedRequest.getOperatorComments());
                request.setApprovalStatus(updatedRequest.getApprovalStatus());

                logger.info("Refund request updated: " + request.toString());
                return new ResponseEntity<>(request, HttpStatus.OK);
            }
        }

        logger.info("No refund request found for userId: " + userId);
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }


    @DeleteMapping("/request/{userId}")
    public ResponseEntity<String> deleteRefundRequest(@PathVariable String userId) {
        logger.info("-------Deleting refund request for userId: " + userId + "-------");

        boolean removed = refundRequests.removeIf(request -> request.getUserId().equals(userId));
        if (removed) {
            logger.info("Refund request successfully deleted for userId: " + userId);
            return new ResponseEntity<>("Refund request deleted", HttpStatus.OK);
        }

        logger.info("No refund request found to delete for userId: " + userId);
        return new ResponseEntity<>("Refund request not found", HttpStatus.NOT_FOUND);
    }
}