package backend.web.core.response.base;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FileResponse {
    private String fileName;
    private byte[] fileData;

    public FileResponse(String fileName, byte[] fileData) {
        this.fileName = fileName;
        this.fileData = fileData;
    }
}
