package com.briefflow.service.briefing;

import com.briefflow.enums.JobType;
import com.briefflow.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BannerBriefingValidator implements TypeBriefingValidator {
    @Override public JobType getType() { return JobType.BANNER; }

    @Override public void validate(Map<String, Object> d) {
        String dim = (String) d.get("dimensions");
        if (dim == null || dim.isBlank())
            throw new BusinessException("BANNER: dimensions é obrigatório");
        String text = (String) d.get("text");
        if (text == null || text.isBlank())
            throw new BusinessException("BANNER: text é obrigatório");
    }
}
