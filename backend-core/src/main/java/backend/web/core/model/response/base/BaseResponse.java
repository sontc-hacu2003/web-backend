package backend.web.core.model.response.base;

import org.apache.logging.log4j.ThreadContext;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BaseResponse {
    public static final String Success = "00";

    private final String traceId = ThreadContext.get("traceId");

    private String code;
    private String desc;
    private Object data;

    private BaseResponse(String code, String desc) {
        this(code, desc, null);
    }

    private BaseResponse(String code, String desc, Object data) {
        this.code = code;
        this.desc = desc;
        this.data = data;
    }

    public static BaseResponse successResponse() {
        return new BaseResponse(Success, "Thành công");
    }

    public static BaseResponse successResponse(String desc) {
        return new BaseResponse(Success, desc);
    }

    public static BaseResponse successResponse(Object data) {
        return new BaseResponse(Success, "Thành công", data);
    }

    public static BaseResponse errorResponse(String desc) {
        return new BaseResponse("99", desc);
    }

    public static BaseResponse validateResponse(String desc) {
        return new BaseResponse("90", desc);
    }

    public static BaseResponse customValidateResponse(String code, String desc) {
        return new BaseResponse(code, desc);
    }

    public static BaseResponse serverError() {
        return new BaseResponse("99", "Lỗi hệ thống");
    }
}
