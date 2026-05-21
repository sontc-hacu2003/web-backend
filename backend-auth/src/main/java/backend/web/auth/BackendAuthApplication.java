package backend.web.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "backend.web")
public class BackendAuthApplication {
    static void main(String[] args) {
        SpringApplication.run(BackendAuthApplication.class, args);
    }
}
