package com.briefflow.service.briefing;

import com.briefflow.enums.JobType;
import com.briefflow.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CarrosselBriefingValidator implements TypeBriefingValidator {
    @Override public JobType getType() { return JobType.CARROSSEL; }

    @Override public void validate(Map<String, Object> d) {
        Object sc = d.get("slideCount");
        if (!(sc instanceof Number n))
            throw new BusinessException("CARROSSEL: slideCount é obrigatório");
        int count = n.intValue();
        if (count < 2 || count > 10)
            throw new BusinessException("CARROSSEL: slideCount deve estar entre 2 e 10");
    }
}
