package com.briefflow.service.briefing;

import com.briefflow.enums.JobType;
import java.util.Map;

public interface TypeBriefingValidator {
    JobType getType();
    void validate(Map<String, Object> data);
}
