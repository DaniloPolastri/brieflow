package com.briefflow.service.briefing;

import com.briefflow.enums.JobType;
import com.briefflow.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class BriefingValidator {

    private final Map<JobType, TypeBriefingValidator> validators;

    public BriefingValidator(List<TypeBriefingValidator> list) {
        this.validators = list.stream().collect(Collectors.toMap(TypeBriefingValidator::getType, v -> v));
    }

    public void validate(JobType type, Map<String, Object> data) {
        if (data == null) throw new BusinessException("briefingData é obrigatório");
        TypeBriefingValidator v = validators.get(type);
        if (v == null) throw new BusinessException("Tipo de job sem validador: " + type);
        v.validate(data);
    }
}
