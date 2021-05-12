package com.backbase.stream.service;

import com.backbase.dbs.approval.api.client.v2.ApprovalTypesApi;
import com.backbase.dbs.approval.api.client.v2.PoliciesApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ApprovalClientService {

    private final PoliciesApi policiesApi;
    private final ApprovalTypesApi approvalTypesApi;

}
