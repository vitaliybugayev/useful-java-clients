import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;

@Slf4j
public class BaseTest {

    @BeforeClass
    public void setUp() {
        log.info("Setting up test environment");
        initRestAssured();
    }

    @AfterClass
    public void tearDown() {
        log.info("Tearing down test environment");
    }

    private static void initRestAssured() {
        RestAssured.requestSpecification = new RequestSpecBuilder()
                .addFilter(new RequestLoggingFilter(LogDetail.METHOD))
                .addFilter(new RequestLoggingFilter(LogDetail.URI))
                .addFilter(new RequestLoggingFilter(LogDetail.BODY))
                .addFilter(new RequestLoggingFilter(LogDetail.HEADERS))
                .addFilter(new RequestLoggingFilter(LogDetail.COOKIES))
                .addFilter(new RequestLoggingFilter(LogDetail.PARAMS))
                .addFilter(new ResponseLoggingFilter(LogDetail.BODY))
                .addFilter(new ResponseLoggingFilter(LogDetail.STATUS))
                .addFilter(new ResponseLoggingFilter(LogDetail.COOKIES))
                .build();
    }
}