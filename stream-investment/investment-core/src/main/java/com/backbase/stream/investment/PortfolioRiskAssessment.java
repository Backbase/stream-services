package com.backbase.stream.investment;

import com.backbase.stream.investment.model.RiskQuestion;
import com.backbase.stream.investment.model.UserRiskAssessment;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class PortfolioRiskAssessment {

    private List<UserRiskAssessment> assessments;
    private List<RiskQuestion> riskQuestions;

}
