package com.briefflow.service.briefing;

import com.briefflow.enums.JobType;
import com.briefflow.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ReelsVideoBriefingValidator implements TypeBriefingValidator {
    @Override public JobType getType() { return JobType.REELS_VIDEO; }

    @Override public void validate(Map<String, Object> d) {
        Object dur = d.get("duration");
        if (!(dur instanceof Number))
            throw new BusinessException("REELS_VIDEO: duration é obrigatório");
        String script = (String) d.get("script");
        if (script == null || script.isBlank())
            throw new BusinessException("REELS_VIDEO: script é obrigatório");
    }
}
