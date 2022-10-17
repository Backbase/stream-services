package com.backbase.stream.portfolio.saga.wealth.portfolio;

import java.util.UUID;
import com.backbase.stream.portfolio.model.Portfolio;
import com.backbase.stream.worker.model.StreamTask;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * WealthPortfolio Task.
 * 
 * @author Vladimir Kirchev
 *
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WealthPortfolioTask extends StreamTask {
	private final Portfolio portfolio;

    public WealthPortfolioTask(Portfolio portfolio) {
		super(UUID.randomUUID().toString());

		this.portfolio = portfolio;
	}

	@Override
	public String getName() {
		return getId();
	}

	public Portfolio getData() {
		return portfolio;
	}
}
