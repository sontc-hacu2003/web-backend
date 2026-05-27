package backend.web.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(scanBasePackages = "backend.web", exclude = UserDetailsServiceAutoConfiguration.class)
public class BackendAuthApplication {
    static void main(String[] args) {
        SpringApplication.run(BackendAuthApplication.class, args);
    }
}
