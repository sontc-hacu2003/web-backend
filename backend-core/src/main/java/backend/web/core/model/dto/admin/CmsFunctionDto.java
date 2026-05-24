package backend.web.core.model.dto.admin;

import backend.web.core.model.entity.admin.CmsFunction;
import lombok.Getter;

import java.util.List;

@Getter
public class CmsFunctionDto {
    private final Long id;
    private final String funcCode;
    private final String funcName;
    private final Long funcOrder;
    private final Long funcDisplay;
    private final Long funcLevel;
    private final Long funcParentId;
    private final String funcUrl;
    private final String funcIcon;
    private final List<CmsFunctionDto> children;

    public CmsFunctionDto(CmsFunction cmsFunction, List<CmsFunctionDto> children) {
        this.id = cmsFunction.getFuncId();
        this.funcCode = cmsFunction.getFuncCode();
        this.funcName = cmsFunction.getFuncName();
        this.funcOrder = cmsFunction.getFuncOrder();
        this.funcDisplay = Long.parseLong(cmsFunction.getFuncDisplay());
        this.funcLevel = cmsFunction.getFuncLevel();
        this.funcParentId = cmsFunction.getParentId();
        this.funcIcon = cmsFunction.getFuncIcon();
        this.funcUrl = cmsFunction.getFuncUrl();
        this.children = children;
    }
}
