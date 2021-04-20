package com.backbase.stream.product.utils;

import com.backbase.stream.legalentity.model.BaseProduct;
import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.CustomDataGroupItem;
import com.backbase.stream.legalentity.model.ProductGroup;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
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
        return getAllProducts(productGroup).map(BaseProduct::getExternalId).collect(Collectors.toList());
    }

    /**
     * Assemble list of all products from a Product Group.
     *
     * @param productGroup Product Group
     * @return List of Internal Ids
     */
    public static List<String> getInternalProductIds(BaseProductGroup productGroup) {
        return getAllProducts(productGroup).map(BaseProduct::getInternalId).collect(Collectors.toList());
    }

    /**
     * Maps all products from each type of product into a flat list of products.
     * @param productGroup Product group
     * @return Stream of the BaseProduct's
     */
    public static Stream<BaseProduct> getAllProducts(BaseProductGroup productGroup) {
        return Stream.of(
            productGroup.getCurrentAccounts(),
            productGroup.getSavingAccounts(),
            productGroup.getDebitCards(),
            productGroup.getCreditCards(),
            productGroup.getLoans(),
            productGroup.getTermDeposits(),
            productGroup.getInvestmentAccounts(),
            productGroup.getCustomProducts())
            .filter(Objects::nonNull)
            .flatMap(List::stream);
    }

    public static <T> Stream<T> nullableCollectionToStream(Collection<T> collection) {
        return Optional.ofNullable(collection)
            .map(Collection::stream)
            .orElseGet(Stream::empty);
    }

    public static List<String> getCustomDataGroupItems(BaseProductGroup productGroup) {
        return nullableCollectionToStream(productGroup.getCustomDataGroupItems())
            .map(CustomDataGroupItem::getInternalId).collect(Collectors.toList());
    }

}
