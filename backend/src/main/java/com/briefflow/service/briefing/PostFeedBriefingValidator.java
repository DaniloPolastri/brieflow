package com.briefflow.service.briefing;

import com.briefflow.enums.JobType;
import com.briefflow.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class PostFeedBriefingValidator implements TypeBriefingValidator {
    private static final Set<String> FORMATS = Set.of("1:1", "4:5");

    @Override public JobType getType() { return JobType.POST_FEED; }

    @Override public void validate(Map<String, Object> d) {
        String caption = (String) d.get("captionText");
        if (caption == null || caption.isBlank())
            throw new BusinessException("POST_FEED: captionText é obrigatório");
        String format = (String) d.get("format");
        if (format == null || !FORMATS.contains(format))
            throw new BusinessException("POST_FEED: format deve ser 1:1 ou 4:5");
    }
}
