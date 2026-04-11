package com.briefflow.unit.service.briefing;

import com.briefflow.enums.JobType;
import com.briefflow.exception.BusinessException;
import com.briefflow.service.briefing.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BriefingValidatorTest {

    private BriefingValidator validator;

    @BeforeEach
    void setUp() {
        validator = new BriefingValidator(List.of(
            new PostFeedBriefingValidator(),
            new StoriesBriefingValidator(),
            new CarrosselBriefingValidator(),
            new ReelsVideoBriefingValidator(),
            new BannerBriefingValidator(),
            new LogoBriefingValidator(),
            new OutrosBriefingValidator()
        ));
    }

    @Test
    void should_acceptValidPostFeedBriefing() {
        Map<String, Object> data = Map.of("captionText", "Hello", "format", "1:1");
        assertDoesNotThrow(() -> validator.validate(JobType.POST_FEED, data));
    }

    @Test
    void should_rejectPostFeed_when_captionTextMissing() {
        Map<String, Object> data = Map.of("format", "1:1");
        assertThrows(BusinessException.class, () -> validator.validate(JobType.POST_FEED, data));
    }

    @Test
    void should_rejectPostFeed_when_formatInvalid() {
        Map<String, Object> data = Map.of("captionText", "x", "format", "9:16");
        assertThrows(BusinessException.class, () -> validator.validate(JobType.POST_FEED, data));
    }

    @Test
    void should_acceptValidStories() {
        Map<String, Object> data = Map.of("text", "Story", "format", "9:16");
        assertDoesNotThrow(() -> validator.validate(JobType.STORIES, data));
    }

    @Test
    void should_rejectStories_when_formatNot9x16() {
        Map<String, Object> data = Map.of("text", "Story", "format", "1:1");
        assertThrows(BusinessException.class, () -> validator.validate(JobType.STORIES, data));
    }

    @Test
    void should_acceptValidCarrossel() {
        Map<String, Object> data = Map.of("slideCount", 5);
        assertDoesNotThrow(() -> validator.validate(JobType.CARROSSEL, data));
    }

    @Test
    void should_acceptMinimalCarrossel_whenOnlySlideCountProvided() {
        Map<String, Object> data = Map.of("slideCount", 5);
        assertDoesNotThrow(() -> validator.validate(JobType.CARROSSEL, data));
    }

    @Test
    void should_rejectCarrossel_when_slideCountBelow2() {
        Map<String, Object> data = Map.of("slideCount", 1);
        assertThrows(BusinessException.class, () -> validator.validate(JobType.CARROSSEL, data));
    }

    @Test
    void should_rejectCarrossel_when_slideCountAbove10() {
        Map<String, Object> data = Map.of("slideCount", 11);
        assertThrows(BusinessException.class, () -> validator.validate(JobType.CARROSSEL, data));
    }

    @Test
    void should_acceptValidReelsVideo() {
        Map<String, Object> data = Map.of("duration", 30, "script", "s");
        assertDoesNotThrow(() -> validator.validate(JobType.REELS_VIDEO, data));
    }

    @Test
    void should_rejectReelsVideo_when_scriptMissing() {
        Map<String, Object> data = Map.of("duration", 30);
        assertThrows(BusinessException.class, () -> validator.validate(JobType.REELS_VIDEO, data));
    }

    @Test
    void should_acceptValidBanner() {
        Map<String, Object> data = Map.of("dimensions", "1920x1080", "text", "t");
        assertDoesNotThrow(() -> validator.validate(JobType.BANNER, data));
    }

    @Test
    void should_rejectBanner_when_dimensionsMissing() {
        Map<String, Object> data = Map.of("text", "t");
        assertThrows(BusinessException.class, () -> validator.validate(JobType.BANNER, data));
    }

    @Test
    void should_acceptValidLogo() {
        Map<String, Object> data = Map.of("desiredStyle", "minimalist");
        assertDoesNotThrow(() -> validator.validate(JobType.LOGO, data));
    }

    @Test
    void should_rejectLogo_when_desiredStyleMissing() {
        assertThrows(BusinessException.class, () -> validator.validate(JobType.LOGO, new HashMap<>()));
    }

    @Test
    void should_acceptValidOutros() {
        Map<String, Object> data = Map.of("freeDescription", "qualquer coisa");
        assertDoesNotThrow(() -> validator.validate(JobType.OUTROS, data));
    }

    @Test
    void should_rejectOutros_when_freeDescriptionBlank() {
        Map<String, Object> data = Map.of("freeDescription", "  ");
        assertThrows(BusinessException.class, () -> validator.validate(JobType.OUTROS, data));
    }
}
