package backend.web.core.response.base;

public record BaseFileResponse(String fileName, byte[] fileData) {
}
