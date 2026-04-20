package com.review;

import com.review.common.enums.*;
import com.review.common.exception.ErrorCode;
import com.review.common.exception.ServiceException;
import com.review.common.result.CommonResult;
import com.review.common.result.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ReviewSystemApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void testCommonResult() {
        CommonResult<String> success = CommonResult.success("hello");
        assertTrue(success.isSuccess());
        assertEquals(0, success.getCode());
        assertEquals("hello", success.getData());

        CommonResult<Void> error = CommonResult.error(ErrorCode.BAD_REQUEST);
        assertFalse(error.isSuccess());
        assertEquals(400, error.getCode());
    }

    @Test
    void testPageResult() {
        PageResult<String> page = PageResult.of(List.of("a", "b"), 10, 1, 5);
        assertEquals(2, page.getList().size());
        assertEquals(10, page.getTotal());
        assertEquals(2, page.getTotalPages());

        PageResult<String> empty = PageResult.empty(1, 10);
        assertTrue(empty.getList().isEmpty());
        assertEquals(0, empty.getTotal());
    }

    @Test
    void testServiceException() {
        ServiceException ex = new ServiceException(ErrorCode.NOT_FOUND);
        assertEquals(404, ex.getCode());
        assertEquals("资源不存在", ex.getMessage());
    }

    @Test
    void testEnums() {
        assertEquals(1, CardCategory.TEXT_BASICS.getCode());
        assertEquals("文本基础核对", CardCategory.TEXT_BASICS.getTitle());
        assertEquals(CardCategory.REDLINES, CardCategory.fromValue(2));

        assertEquals("MARKETING", DocumentType.MARKETING.getValue());
        assertEquals(DocumentType.CONTRACT, DocumentType.fromValue("CONTRACT"));

        assertEquals("PURCHASE", ContractType.PURCHASE.getValue());
        assertEquals(ContractType.NDA, ContractType.fromValue("NDA"));

        assertEquals("PENDING", ReviewStatus.PENDING.getValue());
        assertEquals(ReviewStatus.COMPLETED, ReviewStatus.fromValue("COMPLETED"));

        assertEquals("CRITICAL", Severity.CRITICAL.getValue());
        assertEquals(Severity.MINOR, Severity.fromValue("MINOR"));

        assertEquals("VIOLATION", Verdict.VIOLATION.getValue());
        assertEquals(Verdict.COMPLIANT, Verdict.fromValue("COMPLIANT"));

        assertEquals("TIP", SuggestionType.TIP.getValue());
        assertEquals(SuggestionType.REVISION, SuggestionType.fromValue("REVISION"));

        assertEquals("VERIFIED", CitationStatus.VERIFIED.getValue());
        assertEquals(CitationStatus.PENDING, CitationStatus.fromValue("PENDING"));

        assertEquals("PROHIBITION", NormType.PROHIBITION.getValue());
        assertEquals(NormType.OBLIGATION, NormType.fromValue("OBLIGATION"));

        assertEquals("DEFINITION", ClauseType.DEFINITION.getValue());
        assertEquals(ClauseType.LIABILITY, ClauseType.fromValue("LIABILITY"));

        assertEquals("BANNED_WORD", RuleType.BANNED_WORD.getValue());
        assertEquals(RuleType.NATURAL_LANGUAGE, RuleType.fromValue("NATURAL_LANGUAGE"));

        assertEquals("CONFIRMED", FeedbackAction.CONFIRMED.getValue());
        assertEquals(FeedbackAction.MODIFIED, FeedbackAction.fromValue("MODIFIED"));
    }
}
