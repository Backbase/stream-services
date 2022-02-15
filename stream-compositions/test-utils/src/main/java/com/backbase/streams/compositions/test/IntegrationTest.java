package com.backbase.streams.compositions.test;

import com.backbase.buildingblocks.jwt.core.JsonWebTokenProducerType;
import com.backbase.buildingblocks.jwt.core.exception.JsonWebTokenException;
import com.backbase.buildingblocks.jwt.core.properties.JsonWebTokenProperties;
import com.backbase.buildingblocks.jwt.core.token.JsonWebTokenClaimsSet;
import com.backbase.buildingblocks.jwt.core.type.JsonWebTokenTypeFactory;
import com.backbase.buildingblocks.jwt.internal.token.InternalJwtClaimsSet;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.backbase.buildingblocks.backend.security.auth.config.ServiceApiAuthenticationProperties.DEFAULT_REQUIRED_SCOPE;
import static java.util.Arrays.asList;

@Setter
public abstract class IntegrationTest {

    protected static final String INTERNAL_USER_ID = "internalUerId";

    protected enum TokenType {
        NONE, SERVICE, CLIENT, INTEGRATION,
    }

    private static ThreadLocal<String> token = new ThreadLocal<>();
    private static ThreadLocal<TokenType> tokenType = ThreadLocal.withInitial(() -> TokenType.NONE);

    @Autowired
    private JsonWebTokenProperties tokenProperties;

    @BeforeEach
    public final void setUpToken() throws JsonWebTokenException {
        setUpToken(INTERNAL_USER_ID, TokenType.SERVICE);
    }

    @AfterEach
    public final void clearToken() {
        token.remove();
        tokenType.remove();
    }

    protected String token() {
        return token.get();
    }

    /**
     * Sets token.
     *
     * @param internalUserId
     * @param tokType
     * @throws JsonWebTokenException
     */
    protected final void setUpToken(String internalUserId, TokenType tokType) throws JsonWebTokenException {
        setUpToken(internalUserId, null, tokType);
    }

    /**
     * Sets token.
     *
     * @param internalUserId
     * @param sub
     * @param tokType
     * @throws JsonWebTokenException
     */
    protected final void setUpToken(String internalUserId, String sub, TokenType tokType) throws JsonWebTokenException {
        final Map<String, Object> claims = new HashMap<>();

        if (TokenType.SERVICE.equals(tokType)) {
            claims.put("scope", asList(DEFAULT_REQUIRED_SCOPE));
        } else {
            return;
        }

        @SuppressWarnings("unchecked") final JsonWebTokenProducerType<JsonWebTokenClaimsSet, String> tokenFactory =
                JsonWebTokenTypeFactory.getProducer(this.tokenProperties);

        claims.put(InternalJwtClaimsSet.INTERNAL_USER_ID, internalUserId);
        claims.put("exp", System.currentTimeMillis() + 86400 * 365);
        if (sub != null) {
            claims.put(InternalJwtClaimsSet.SUBJECT_CLAIM, sub);
        }

        token.set("Bearer " + tokenFactory.createToken(new InternalJwtClaimsSet(claims)));
        tokenType.set(tokType);
    }

    /**
     * Reads file's content from classpath.
     *
     * @param resourcePath Resource path
     * @return File's content
     * @throws IOException
     */
    protected String readContentFromClasspath(String resourcePath)
            throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(resourcePath).getFile());
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }
}

