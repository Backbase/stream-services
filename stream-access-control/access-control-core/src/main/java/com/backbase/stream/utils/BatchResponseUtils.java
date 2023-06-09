package com.backbase.stream.utils;

import java.text.MessageFormat;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
public class BatchResponseUtils {

    @Value("${backbase.stream.dbs.batch.validate-atomic-response:true}")
    private boolean validateAtomicResponse = true;

    /**
     * Utility method to check Batch Response Item and throw WebClientResponseException if response item status is not
     * 200.
     *
     * @param response   original Batch Response Item object.
     * @param operation  batch operation which was executed to get the response.
     * @param status     status code of the batch response item.
     * @param resourceId identifier of affected resource.
     * @param errors     list of errors.
     * @return original response.
     */
    public <T> T checkBatchResponseItem(
        T response, String operation, String status, String resourceId, List<String> errors) {
        log.debug(
            "Batch {} response: status {} for resource {}, errors: {}",
            operation,
            status,
            resourceId,
            errors);
        if (status != null && !HttpStatus.valueOf(Integer.parseInt(status)).is2xxSuccessful()) {
            String statusText =
                MessageFormat.format(
                    "Failed item in the batch for {0}: status {1} for resource {2}, errors:" + " {3}",
                    operation, status, resourceId, errors);
            if (validateAtomicResponse) {
                throw new WebClientResponseException(
                    Integer.parseInt(status), statusText, null, null, null);
            } else {
                log.error("Inconsistent batch response item: {}", statusText);
            }
        }
        return response;
    }
}
