package backend.web.core.config.database;

import lombok.Getter;

import java.util.Objects;

@Getter
public abstract class Database {
    private final String url;
    private final String username;
    private final String password;

    protected Database(String url, String username, String password) {
        this.url = requireText(url, "url");
        this.username = requireText(username, "username");
        this.password = Objects.requireNonNull(password, "password must not be null");
    }

    public abstract String getDriverClassName();

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
