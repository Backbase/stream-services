package com.backbase.stream.investment.model;

import com.backbase.investment.api.service.v1.model.Status351Enum;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRiskAssessment {

    private String userName;
    private List<String> choices = new ArrayList<>();
    private Status351Enum status;
    private Map<String, Object> flatAttributes;
    private Map<String, String> extraData = new HashMap<>();

}
