package backend.web.core.model.response.base;

public record BaseFileResponse(String fileName, byte[] fileData) {
}
