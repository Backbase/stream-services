package com.backbase.stream.limit;

import com.backbase.dbs.limit.api.service.v2.model.CreateLimitRequestBody;
import com.backbase.dbs.limit.api.service.v2.model.LimitsPostResponseBody;
import com.backbase.stream.worker.model.StreamTask;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class LimitsTask extends StreamTask {

  private CreateLimitRequestBody data;
  private LimitsPostResponseBody response;

  public LimitsTask(String unitOfWorkId, CreateLimitRequestBody data) {
    super(unitOfWorkId);
    this.data = data;
  }

  @Override
  public String getName() {
    return "limit";
  }
}
