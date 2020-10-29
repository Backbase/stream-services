import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.web.reactive.function.BodyInserters.fromValue;


import com.backbase.stream.LegalEntityHttpApplication;
import com.backbase.stream.LegalEntitySaga;
import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@ActiveProfiles("it")
@AutoConfigureWebTestClient
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = LegalEntityHttpApplication.class)
public class LegalEntityControllerIT {

    @MockBean
    private LegalEntitySaga legalEntityService;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void tracingSuccessResponseHeadersTest() {
        LegalEntity legalEntity = createLegalEntity();
        LegalEntityTask legalEntityTask = new LegalEntityTask(legalEntity);

        when(legalEntityService.executeTask(any())).thenReturn(Mono.just(legalEntityTask));

        webTestClient.post().uri("/legal-entity")
            .body(fromValue(legalEntity))
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists("X-B3-TraceId")
            .returnResult(String.class)
            .getResponseBody()
            .blockFirst();
    }

    @Test
    void tracingFailedResponseHeadersTest() {
        when(legalEntityService.executeTask(any())).thenReturn(Mono.error(new Exception()));

        webTestClient.post().uri("/legal-entity")
            .body(fromValue(createLegalEntity()))
            .exchange()
            .expectStatus().is5xxServerError()
            .expectHeader().exists("X-B3-TraceId")
            .returnResult(String.class)
            .getResponseBody()
            .blockFirst();
    }

    private LegalEntity createLegalEntity(){
        return new LegalEntity()
            .name("name")
            .externalId("externalId")
            .legalEntityType(LegalEntityType.CUSTOMER);
    }

}
