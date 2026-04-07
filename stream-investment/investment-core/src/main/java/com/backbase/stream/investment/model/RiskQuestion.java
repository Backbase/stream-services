package com.backbase.stream.investment.model;

import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RiskQuestion {

    private UUID uuid;
    private String code;
    private Integer order;
    private String description;
    private Double score;
    private List<QuestionChoice> choices;

}
