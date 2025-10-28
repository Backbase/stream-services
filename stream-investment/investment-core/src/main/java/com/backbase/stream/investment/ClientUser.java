package com.backbase.stream.investment;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ClientUser {

    private UUID investmentClientId;
    private String internalUserId;
    private String externalUserId;
    private String legalEntityExternalId;


}
