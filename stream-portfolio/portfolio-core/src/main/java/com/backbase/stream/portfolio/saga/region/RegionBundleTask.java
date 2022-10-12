package com.backbase.stream.portfolio.saga.region;

import java.util.UUID;

import com.backbase.stream.portfolio.model.RegionBundle;
import com.backbase.stream.worker.model.StreamTask;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * RegionBundle Task.
 * 
 * @author Vladimir Kirchev
 *
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RegionBundleTask extends StreamTask {
	private final RegionBundle regionBundle;

	public RegionBundleTask(RegionBundle regionBundle) {
		super(UUID.randomUUID().toString());

		this.regionBundle = regionBundle;
	}

	@Override
	public String getName() {
		return getId();
	}
	
	public RegionBundle getData() {
        return regionBundle;
    }
}
