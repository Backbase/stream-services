package com.backbase.streams.compositions.test;

import com.backbase.buildingblocks.jwt.core.JsonWebTokenProducerType;
import com.backbase.buildingblocks.jwt.core.exception.JsonWebTokenException;
import com.backbase.buildingblocks.jwt.core.properties.JsonWebTokenProperties;
import com.backbase.buildingblocks.jwt.core.token.JsonWebTokenClaimsSet;
import com.backbase.buildingblocks.jwt.core.type.JsonWebTokenTypeFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AbstractIntegrationTest {

    @Mock
    JsonWebTokenProperties jsonWebTokenProperties;

    @Test
    void testToken() throws JsonWebTokenException {
        SampleTest test = new SampleTest();
        test.setTokenProperties(jsonWebTokenProperties);

        JsonWebTokenProducerType<JsonWebTokenClaimsSet, String> tokenFactory = tokenData -> "token";

        Mockito.mockStatic(JsonWebTokenTypeFactory.class);
        when(JsonWebTokenTypeFactory.getProducer(any())).thenReturn(tokenFactory);

        assertNull(test.token());
        test.setUpToken();

        assertEquals("Bearer token", test.token());
        test.clearToken();
        assertNull(test.token());

        test.setUpToken("userId", IntegrationTest.TokenType.SERVICE);
        assertEquals("Bearer token", test.token());
    }

    @Test
    void testReadResource() throws IOException {
        SampleTest test = new SampleTest();
        assertTrue(test.readContentFromClasspath("test.json").startsWith("{}"));
    }
}

class SampleTest extends IntegrationTest {
    public String readContentFromClasspath(String resourcePath) throws IOException {
        return super.readContentFromClasspath(resourcePath);
    }
}
