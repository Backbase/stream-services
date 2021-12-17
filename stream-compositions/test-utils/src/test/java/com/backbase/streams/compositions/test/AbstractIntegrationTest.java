package com.backbase.streams.compositions.test;

import com.backbase.buildingblocks.jwt.core.JsonWebTokenProducerType;
import com.backbase.buildingblocks.jwt.core.exception.JsonWebTokenException;
import com.backbase.buildingblocks.jwt.core.properties.JsonWebTokenProperties;
import com.backbase.buildingblocks.jwt.core.token.JsonWebTokenClaimsSet;
import com.backbase.buildingblocks.jwt.core.type.JsonWebTokenTypeFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AbstractIntegrationTest {

    @Mock
    JsonWebTokenProperties jsonWebTokenProperties;

    @Test
    void test() throws JsonWebTokenException {
        SampleTest test = new SampleTest();
        test.setTokenProperties(jsonWebTokenProperties);

        JsonWebTokenProducerType<JsonWebTokenClaimsSet, String> tokenFactory = new JsonWebTokenProducerType<JsonWebTokenClaimsSet, String>() {
            @Override
            public String createToken(JsonWebTokenClaimsSet tokenData) throws JsonWebTokenException {
                return "token";
            }
        };

        MockedStatic<JsonWebTokenTypeFactory> jsonWebTokenTypeFactory = Mockito.mockStatic(JsonWebTokenTypeFactory.class);
        when(JsonWebTokenTypeFactory.getProducer(any())).thenReturn(tokenFactory);

        Assertions.assertNull(test.token());
        test.setUpToken();

        Assertions.assertEquals("Bearer token", test.token());
        test.clearToken();
        Assertions.assertNull(test.token());

        test.setUpToken("userId", IntegrationTest.TokenType.SERVICE);
        Assertions.assertEquals("Bearer token", test.token());

    }
}

class SampleTest extends IntegrationTest {

}
