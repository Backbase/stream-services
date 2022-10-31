import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class HealthCheck {

    private final static String DEFAULT_URI = "http://localhost:8080/actuator/health/readiness";
    private final static String DEFAULT_HEALTH_INDICATOR = "UP";
    private final static long DEFAULT_TIMEOUT = 10;

    public static void main(String[] args) {
        var uri = DEFAULT_URI;
        if (args.length >= 1) {
            uri = args[0];
        }

        var healthIndicator = DEFAULT_HEALTH_INDICATOR;
        if (args.length >= 2) {
            healthIndicator = args[1];
        }

        healthCheck(uri, healthIndicator);
    }

    private static void healthCheck(String uri, String healthIndicator) {
        try {
            System.out.println("Performing health check on " + uri + " with health indicator: " + healthIndicator);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(uri))
                .timeout(Duration.of(DEFAULT_TIMEOUT, ChronoUnit.SECONDS))
                .GET()
                .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("Service unavailable, code: " + response.statusCode());
                System.exit(6);
            }

            if (!response.body().toUpperCase().contains(healthIndicator.toUpperCase())) {
                System.out.println("Service unhealthy: " + response.body());
                System.exit(11);
            }

            System.out.println("Service healthy: " + response.body());
        } catch (Exception e) {
            System.out.println("Service unreachable");
            System.exit(6);
        }
    }

}
