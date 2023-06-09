package com.backbase.stream.product.task;

import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.worker.model.StreamTask;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class ProductGroupTask extends StreamTask {

    private ProductGroup productGroup;

    public ProductGroupTask(String id, ProductGroup productGroup) {
        super(id);
        this.productGroup = productGroup;
    }

    public ProductGroupTask(ProductGroup productGroup) {
        super(productGroup.getName());
        this.productGroup = productGroup;
    }

    public ProductGroup getData() {
        return productGroup;
    }

    public ProductGroupTask data(ProductGroup productGroup) {
        this.productGroup = productGroup;
        return this;
    }

    @Override
    public String getName() {
        return getId();
    }
}
