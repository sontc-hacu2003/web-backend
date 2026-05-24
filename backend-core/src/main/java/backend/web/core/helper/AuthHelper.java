package backend.web.core.helper;

import backend.web.core.model.dto.admin.CmsFunctionDto;
import backend.web.core.model.entity.admin.CmsFunction;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class AuthHelper {
    public static List<CmsFunctionDto> getChildrenFunction(CmsFunction function, List<CmsFunction> allFunctions) {
        return allFunctions.stream()
                .filter(x -> Objects.equals(x.getParentId(), function.getFuncId()))
                .sorted(Comparator.comparing(CmsFunction::getFuncOrder, Comparator.nullsLast(Long::compareTo)))
                .map(x -> new CmsFunctionDto(x, getChildrenFunction(x, allFunctions)))
                .toList();
    }
}
