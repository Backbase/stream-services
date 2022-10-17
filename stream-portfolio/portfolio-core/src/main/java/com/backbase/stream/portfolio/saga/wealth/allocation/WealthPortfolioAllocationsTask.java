package com.backbase.stream.portfolio.saga.wealth.allocation;

import java.util.UUID;

import com.backbase.stream.portfolio.model.AllocationBundle;
import com.backbase.stream.worker.model.StreamTask;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * WealthPortfolioAllocations Task.
 * 
 * @author Vladimir Kirchev
 *
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WealthPortfolioAllocationsTask extends StreamTask {
	private final AllocationBundle allocationBundle;

	public WealthPortfolioAllocationsTask(AllocationBundle allocationBundle) {
		super(UUID.randomUUID().toString());

		this.allocationBundle = allocationBundle;
	}

	@Override
	public String getName() {
		return getId();
	}

	public AllocationBundle getData() {
		return allocationBundle;
	}

}
