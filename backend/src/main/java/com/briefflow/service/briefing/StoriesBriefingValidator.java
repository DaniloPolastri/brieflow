package com.briefflow.service.briefing;

import com.briefflow.enums.JobType;
import com.briefflow.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StoriesBriefingValidator implements TypeBriefingValidator {
    @Override public JobType getType() { return JobType.STORIES; }

    @Override public void validate(Map<String, Object> d) {
        String text = (String) d.get("text");
        if (text == null || text.isBlank())
            throw new BusinessException("STORIES: text é obrigatório");
        String format = (String) d.get("format");
        if (!"9:16".equals(format))
            throw new BusinessException("STORIES: format deve ser 9:16");
    }
}
