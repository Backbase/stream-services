package com.backbase.stream.exceptions;

import com.backbase.stream.approval.model.PolicyAssignment;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class PolicyAssignmentException extends RuntimeException {

  private final PolicyAssignment policyAssignment;
  private final String message;
  private final String httpResponse;

  public PolicyAssignmentException(
      PolicyAssignment policyAssignment, String message, WebClientResponseException exception) {

    super(exception);
    httpResponse = exception.getResponseBodyAsString();
    this.policyAssignment = policyAssignment;
    this.message = message;
  }

  public String getHttpResponse() {
    return httpResponse;
  }
}
