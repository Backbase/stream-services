package com.backbase.stream.product.utils;

import com.backbase.stream.legalentity.model.BaseProduct;
import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.CustomDataGroupItem;
import com.backbase.stream.legalentity.model.ProductGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.experimental.UtilityClass;

/**
 * Utility class for Stream Access Control.
 */
@UtilityClass
public class StreamUtils {


    /**
     * Assemble list of all products from a Product Group.
     *
     * @param productGroup Product Group
     * @return List of Internal Ids
     */
    public static List<String> getExternalProductIds(ProductGroup productGroup) {
        return getAllProducts(productGroup).stream().map(BaseProduct::getExternalId).collect(Collectors.toList());
    }

    /**
     * Assemble list of all products from a Product Group.
     *
     * @param productGroup Product Group
     * @return List of Internal Ids
     */
    public static List<String> getInternalProductIds(BaseProductGroup productGroup) {
        return getAllProducts(productGroup).stream().map(BaseProduct::getInternalId).collect(Collectors.toList());
    }

    public static List<BaseProduct> getAllProducts(BaseProductGroup productGroup) {
        List<BaseProduct> products = new ArrayList<>();
        if (productGroup.getCurrentAccounts() != null) {
            products.addAll(productGroup.getCurrentAccounts());
        }
        if (productGroup.getSavingAccounts() != null) {
            products.addAll(productGroup.getSavingAccounts());
        }
        if (productGroup.getDebitCards() != null) {
            products.addAll(productGroup.getDebitCards());
        }
        if (productGroup.getCreditCards() != null) {
            products.addAll(productGroup.getCreditCards());
        }
        if (productGroup.getLoans() != null) {
            products.addAll(productGroup.getLoans());
        }
        if (productGroup.getTermDeposits() != null) {
            products.addAll(productGroup.getTermDeposits());
        }
        if (productGroup.getInvestmentAccounts() != null) {
            products.addAll(productGroup.getInvestmentAccounts());
        }
        if (productGroup.getCustomProducts() != null) {
            products.addAll(productGroup.getCustomProducts());
        }
        return products;
    }

    public static <T> Stream<T> nullableCollectionToStream(Collection<T> collection) {
        return Optional.ofNullable(collection).map(
                Collection::stream)
                .orElseGet(Stream::empty);
    }

    public static List<String> getCustomDataGroupItems(BaseProductGroup productGroup) {
        return productGroup.getCustomDataGroupItems().stream()
            .map(CustomDataGroupItem::getInternalId).collect(Collectors.toList());
    }

}
