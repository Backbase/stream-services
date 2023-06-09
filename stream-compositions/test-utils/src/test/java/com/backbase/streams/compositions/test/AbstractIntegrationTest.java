package com.backbase.streams.compositions.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backbase.buildingblocks.jwt.core.JsonWebTokenProducerType;
import com.backbase.buildingblocks.jwt.core.exception.JsonWebTokenException;
import com.backbase.buildingblocks.jwt.core.properties.JsonWebTokenProperties;
import com.backbase.buildingblocks.jwt.core.token.JsonWebTokenClaimsSet;
import com.backbase.buildingblocks.jwt.core.type.JsonWebTokenTypeFactory;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AbstractIntegrationTest {

  @Mock JsonWebTokenProperties jsonWebTokenProperties;

  @Test
  void testToken() throws JsonWebTokenException {
    SampleIT sampleIT = new SampleIT();
    sampleIT.setTokenProperties(jsonWebTokenProperties);

    JsonWebTokenProducerType<JsonWebTokenClaimsSet, String> tokenFactory = tokenData -> "token";

    Mockito.mockStatic(JsonWebTokenTypeFactory.class);
    when(JsonWebTokenTypeFactory.getProducer(any())).thenReturn(tokenFactory);

    assertNull(sampleIT.token());
    sampleIT.setUpToken();

    assertEquals("Bearer token", sampleIT.token());
    sampleIT.clearToken();
    assertNull(sampleIT.token());

    sampleIT.setUpToken("userId", IntegrationTest.TokenType.SERVICE);
    assertEquals("Bearer token", sampleIT.token());
  }

  @Test
  void testReadResource() throws IOException {
    SampleIT test = new SampleIT();
    assertTrue(test.readContentFromClasspath("test.json").startsWith("{}"));
  }

  @Test
  void testTokenConverterServer() throws IOException {
    SampleIT test = new SampleIT();
    test.startTokenConverterServer();
    test.stopTokenConverterServer();
  }
}

class SampleIT extends IntegrationTest {

  public String readContentFromClasspath(String resourcePath) throws IOException {
    return super.readContentFromClasspath(resourcePath);
  }
}
