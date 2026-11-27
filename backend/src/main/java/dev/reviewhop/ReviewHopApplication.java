package dev.reviewhop;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(
        info = @Info(
                title = "ReviewHop API",
                version = "v1",
                description = "API for scheduling reviews of completed learning tasks"))
@SpringBootApplication
public class ReviewHopApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReviewHopApplication.class, args);
    }
}
