package com.review.module.parser;

import com.review.module.parser.dto.CrossReference;
import com.review.module.parser.dto.ParsedClause;
import com.review.module.parser.dto.ParsedContract;
import com.review.module.parser.impl.ContractClauseParserImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContractClauseParserTest {

    private ContractClauseParserImpl parser;

    @BeforeEach
    void setUp() {
        parser = new ContractClauseParserImpl();
    }

    @Test
    void testClauseHierarchyParsing() {
        String content = """
                第一条 合同目的
                本合同旨在明确双方的权利和义务。
                第二条 合同金额
                合同总金额为人民币壹佰万元整。
                第三条 违约责任
                任何一方违约应当赔偿对方损失。
                """;

        ParsedContract result = parser.parse(content);
        List<ParsedClause> clauses = result.getClauses();

        assertNotNull(clauses);
        assertEquals(3, clauses.size());
        assertEquals("1", clauses.get(0).getClauseNumber());
        assertEquals("2", clauses.get(1).getClauseNumber());
        assertEquals("3", clauses.get(2).getClauseNumber());
        assertTrue(clauses.get(0).getClauseTitle().contains("合同目的"));
    }

    @Test
    void testDefinitionExtraction() {
        String content = """
                第一条 定义
                本合同所称"甲方"是指合同的买方。
                本合同所称"乙方"是指合同的卖方。
                """;

        ParsedContract result = parser.parse(content);
        Map<String, String> definitions = result.getDefinitions();

        assertNotNull(definitions);
        assertFalse(definitions.isEmpty());
        assertTrue(definitions.containsKey("甲方"));
        assertTrue(definitions.containsKey("乙方"));
    }

    @Test
    void testCrossReferenceDetection() {
        String content = """
                第一条 合同目的
                本合同旨在明确双方的权利和义务。
                第二条 付款方式
                付款方式依照本合同第一条执行。
                """;

        ParsedContract result = parser.parse(content);
        List<CrossReference> refs = result.getCrossReferences();

        assertNotNull(refs);
        assertFalse(refs.isEmpty());
        assertEquals("2", refs.get(0).getSourceClause());
        assertTrue(refs.get(0).getReferenceText().contains("第一条"));
    }

    @Test
    void testEmptyContent() {
        ParsedContract result = parser.parse("");
        assertNotNull(result);
        assertTrue(result.getClauses().isEmpty());
        assertTrue(result.getDefinitions().isEmpty());
        assertTrue(result.getCrossReferences().isEmpty());
    }

    @Test
    void testNullContent() {
        ParsedContract result = parser.parse(null);
        assertNotNull(result);
        assertTrue(result.getClauses().isEmpty());
    }

    @Test
    void testClauseTypeInference() {
        String content = """
                第一条 定义与释义
                本条对合同术语进行定义和释义。
                第二条 违约赔偿
                违约方应当赔偿损失并支付违约金。
                """;

        ParsedContract result = parser.parse(content);
        List<ParsedClause> clauses = result.getClauses();

        assertEquals(2, clauses.size());
        assertEquals("DEFINITION", clauses.get(0).getClauseType());
        assertEquals("LIABILITY", clauses.get(1).getClauseType());
    }
}
