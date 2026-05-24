package backend.web.core.response.base;

import java.util.function.Supplier;
import java.util.stream.Stream;

import lombok.Getter;

@Getter
public class LogStreamResult<T> {
    private final Supplier<Stream<?>> supplier;
    private final long total;

    public LogStreamResult(Supplier<Stream<?>> supplier, long total) {
        this.supplier = supplier;
        this.total = total;
    }
}
