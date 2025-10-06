package com.backbase.stream.investment;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Data
public class InvestmentData {

    private List<ClientUser> clientUsers;

}
