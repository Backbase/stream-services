package com.backbase.stream.portfolio.saga.wealth.subportfolio;

import java.util.UUID;
import com.backbase.stream.portfolio.model.SubPortfolioBundle;
import com.backbase.stream.worker.model.StreamTask;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * WealthSubPortfolio Task.
 * 
 * @author Vladimir Kirchev
 *
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WealthSubPortfolioTask extends StreamTask {
    private final SubPortfolioBundle subPortfolioBundle;

    public WealthSubPortfolioTask(SubPortfolioBundle subPortfolioBundle) {
        super(UUID.randomUUID().toString());

        this.subPortfolioBundle = subPortfolioBundle;
    }

    @Override
    public String getName() {
        return getId();
    }

    public SubPortfolioBundle getData() {
        return subPortfolioBundle;
    }
}
