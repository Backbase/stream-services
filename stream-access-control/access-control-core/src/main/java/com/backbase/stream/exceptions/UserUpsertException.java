package com.backbase.stream.exceptions;

import com.backbase.dbs.user.api.service.v2.model.BatchResponseItem;
import com.backbase.stream.legalentity.model.User;
import java.util.List;
import lombok.Getter;

@Getter
public class UserUpsertException extends RuntimeException {

  private final List<User> users;
  private final List<BatchResponseItem> batchResponseItem;

  public UserUpsertException(
      String s, List<User> current, List<BatchResponseItem> batchResponseItem) {
    this.users = current;
    this.batchResponseItem = batchResponseItem;
  }
}
