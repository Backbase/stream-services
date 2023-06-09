package com.backbase.stream.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.backbase.dbs.accesscontrol.api.service.v2.model.FunctionGroupItem;
import com.backbase.dbs.accesscontrol.api.service.v2.model.Permission;
import com.backbase.dbs.accesscontrol.api.service.v2.model.Privilege;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class BusinessFunctionGroupMapperTest {

    private final BusinessFunctionGroupMapper mapper = new BusinessFunctionGroupMapperImpl();

    @Test
    void testMap() {
        // When
        var result = mapper.map(createFunctionGroupItem());

        // Then
        assertNotNull(result);
        assertEquals("1001", result.getId());
        assertEquals("S001", result.getServiceAgreementId());
    }

    @Test
    void testMapBusinessFunction() {
        // When
        var result = mapper.map(createFunctionGroupItem());

        // Then
        assertNotNull(result);
        assertEquals("F001", result.getFunctions().get(0).getFunctionId());
        assertEquals("CREATE", result.getFunctions().get(0).getPrivileges().get(0).getPrivilege());
    }

    private FunctionGroupItem createFunctionGroupItem() {
        FunctionGroupItem item = new FunctionGroupItem();
        item.setId("1001");
        item.setServiceAgreementId("S001");
        item.setName("Payments");
        item.setPermissions(Collections.singletonList(createPermisson()));
        return item;
    }

    private Permission createPermisson() {
        Permission permission = new Permission();
        permission.setFunctionId("F001");
        permission.setAssignedPrivileges(
            Collections.singletonList(new Privilege().privilege("CREATE")));
        return permission;
    }
}
