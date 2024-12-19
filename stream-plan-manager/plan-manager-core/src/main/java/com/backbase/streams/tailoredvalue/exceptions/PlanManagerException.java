package com.backbase.streams.tailoredvalue.exceptions;

import lombok.Getter;

@Getter
public class PlanManagerException extends RuntimeException {

  private final String message;

  public PlanManagerException(Throwable exception, String message) {
    super(message, exception);
    this.message = message;
  }

  public PlanManagerException(String message) {
    super(message);
    this.message = message;
  }
}
