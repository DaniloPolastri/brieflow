package com.briefflow.service.briefing;

import com.briefflow.enums.JobType;
import com.briefflow.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LogoBriefingValidator implements TypeBriefingValidator {
    @Override public JobType getType() { return JobType.LOGO; }

    @Override public void validate(Map<String, Object> d) {
        String style = (String) d.get("desiredStyle");
        if (style == null || style.isBlank())
            throw new BusinessException("LOGO: desiredStyle é obrigatório");
    }
}
