package com.briefflow.service.briefing;

import com.briefflow.enums.JobType;
import com.briefflow.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OutrosBriefingValidator implements TypeBriefingValidator {
    @Override public JobType getType() { return JobType.OUTROS; }

    @Override public void validate(Map<String, Object> d) {
        String desc = (String) d.get("freeDescription");
        if (desc == null || desc.isBlank())
            throw new BusinessException("OUTROS: freeDescription é obrigatório");
    }
}
