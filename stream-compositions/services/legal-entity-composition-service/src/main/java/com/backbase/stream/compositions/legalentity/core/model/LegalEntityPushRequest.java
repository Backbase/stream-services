package com.backbase.stream.compositions.legalentity.core.model;

import com.backbase.stream.legalentity.model.LegalEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LegalEntityPushRequest {

  private LegalEntity legalEntity;
}