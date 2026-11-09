package com.backbase.stream.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.backbase.accesscontrol.functiongroup.api.service.v1.model.FunctionGroupItem;
import com.backbase.accesscontrol.functiongroup.api.service.v1.model.FunctionGroupItem.TypeEnum;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FunctionGroupUtilsTest {

    private static final String NAME = "US SMB Plan - Admin User - Business Context - Functional Permissions";

    @Test
    void returnsEmptyMapForNullInput() {
        assertTrue(FunctionGroupUtils.functionGroupsByName(null, "sa").isEmpty());
    }

    @Test
    void keepsUniqueNamesRegardlessOfType() {
        Map<String, FunctionGroupItem> result = FunctionGroupUtils.functionGroupsByName(List.of(
            fg("custom-id", "Local Role", TypeEnum.CUSTOM, "sa"),
            fg("untyped-id", "Untyped Role", null, "sa"),
            fg("ref-id", "Reference Role", TypeEnum.REFERENCE, "msa")), "sa");

        assertEquals(3, result.size());
        assertEquals("custom-id", result.get("Local Role").getId());
        assertEquals("untyped-id", result.get("Untyped Role").getId());
        assertEquals("ref-id", result.get("Reference Role").getId());
    }

    @Test
    void prefersReferenceWhenLocalCopyHasSameName() {
        Map<String, FunctionGroupItem> result = FunctionGroupUtils.functionGroupsByName(List.of(
            fg("local-copy-id", NAME, TypeEnum.CUSTOM, "sa"),
            fg("reference-id", NAME, TypeEnum.REFERENCE, "msa")), "sa");

        assertEquals(1, result.size());
        assertEquals("reference-id", result.get(NAME).getId());
    }

    @Test
    void fallsBackToFirstWhenNoReferenceAmongDuplicates() {
        Map<String, FunctionGroupItem> result = FunctionGroupUtils.functionGroupsByName(List.of(
            fg("first-id", NAME, TypeEnum.CUSTOM, "sa"),
            fg("second-id", NAME, TypeEnum.SYSTEM, "sa")), "sa");

        assertEquals("first-id", result.get(NAME).getId());
    }

    @Test
    void ignoresNullItemsAndNullNames() {
        Map<String, FunctionGroupItem> result = FunctionGroupUtils.functionGroupsByName(Arrays.asList(
            null,
            fg("no-name-id", null, TypeEnum.CUSTOM, "sa"),
            fg("ref-id", NAME, TypeEnum.REFERENCE, "msa")), "sa");

        assertEquals(1, result.size());
        assertEquals("ref-id", result.get(NAME).getId());
    }

    private static FunctionGroupItem fg(String id, String name, TypeEnum type, String serviceAgreementId) {
        return new FunctionGroupItem().id(id).name(name).type(type).serviceAgreementId(serviceAgreementId);
    }
}
