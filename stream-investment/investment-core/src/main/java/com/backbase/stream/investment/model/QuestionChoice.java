package com.backbase.stream.investment.model;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionChoice {

    private UUID uuid;
    private String code;
    private String description;
    private Double score;
    private Boolean suitable;
    private Integer order;

}
