package com.backbase.stream.portfolio.saga.wealth.asset;

import java.util.UUID;
import com.backbase.stream.portfolio.model.AssetClassBundle;
import com.backbase.stream.worker.model.StreamTask;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * WealthAssets Task.
 * 
 * @author Vladimir Kirchev
 *
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WealthAssetsTask extends StreamTask {
    private final AssetClassBundle assetClassBundle;

    public WealthAssetsTask(AssetClassBundle assetClassBundle) {
        super(UUID.randomUUID().toString());

        this.assetClassBundle = assetClassBundle;
    }

    @Override
    public String getName() {
        return getId();
    }

    public AssetClassBundle getData() {
        return assetClassBundle;
    }
}
