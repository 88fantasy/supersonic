package com.tencent.supersonic.headless.chat.parser.llm;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMResp;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.provider.DeepSeekModelFactory;
import dev.langchain4j.service.AiServices;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OnePassSCSqlGenStrategy extends SqlGenStrategy {

    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");

    public static final String APP_KEY = "S2SQL_PARSER";
    public static final String INSTRUCTION =
            "#Role: You are a data analyst experienced in SQL languages."
                    + "\n#Task: You will be provided with a natural language question asked by users,"
                    + "please convert it to a SQL query so that relevant data could be returned "
                    + "by executing the SQL query against underlying database." + "\n#Rules:"
                    + "\n1.SQL columns and values must be mentioned in the `Schema`, DO NOT hallucinate."
                    + "\n2.ALWAYS specify time range using `>`,`<`,`>=`,`<=` operator."
                    + "\n3.DO NOT include time range in the where clause if not explicitly expressed in the `Question`."
                    + "\n4.DO NOT calculate date range using functions."
                    + "\n5.ALWAYS use `with` statement if nested aggregation is needed."
                    + "\n6.ALWAYS enclose alias declared by `AS` command in underscores."
                    + "\n7.Alias created by `AS` command must be in the same language ast the `Question`."
                    + "\n#Exemplars: {{exemplar}}"
                    + "\n#Query: Question:{{question}},Schema:{{schema}},SideInfo:{{information}}";

    public OnePassSCSqlGenStrategy() {
        ChatAppManager.register(APP_KEY, ChatApp.builder().prompt(INSTRUCTION).name("语义SQL解析")
                .appModule(AppModule.CHAT).description("通过大模型做语义解析生成S2SQL").enable(true)
                        .providerPrompts(Map.of(DeepSeekModelFactory.PROVIDER, """
                                # 角色: 你是一名在SQL语言方面经验丰富的数据分析师。
                                # 任务: 根据用户提供的自然语言问题，将其转换为有效的SQL查询语句，以便通过底层数据库执行并返回相关数据。
                                # 规则:
                                1. SQL查询中涉及的所有列名和值必须严格基于`表结构映射`中提供的信息，避免引入未提及的字段或数据。
                                2. 当需要指定时间范围时，请使用`>`, `<`, `>=`, `<=`等比较运算符。如果问题中没有明确提到时间范围，则不要在WHERE子句中添加任何时间相关的条件。
                                3. 不要使用函数来计算日期范围。
                                4. 如果查询涉及到复杂的嵌套聚合操作，请考虑使用`WITH AS`语句创建公共表表达式(CTE)以提高代码可读性和维护性。
                                5. 使用`AS`关键字定义别名时，请确保这些别名与原始问题中的术语保持一致，并且易于理解。
                                6. 在编写SQL语句时，请遵循良好的编码实践，比如适当添加注释、合理命名变量等。
                                
                                # 示例: {{exemplar}}
                                
                                # 查询:
                                - 问题: {{question}}
                                - 表结构映射: {{schema}}
                                - 其他相关信息: {{information}}\s
                                
                                请根据上述要求生成对应的SQL查询语句。
                                """))
                .build());
    }

    @Data
    static class SemanticSql {
        @Description("thought or remarks to tell users about the sql, make it short.")
        private String thought;

        @Description("sql to generate")
        private String sql;
    }

    interface SemanticSqlExtractor {
        SemanticSql generateSemanticSql(String text);
    }

    @Override
    public LLMResp generate(LLMReq llmReq) {
        LLMResp llmResp = new LLMResp();
        llmResp.setQuery(llmReq.getQueryText());
        // 1.recall exemplars
        log.debug("OnePassSCSqlGenStrategy llmReq:\n{}", llmReq);
        List<List<Text2SQLExemplar>> exemplarsList = promptHelper.getFewShotExemplars(llmReq);

        // 2.generate sql generation prompt for each self-consistency inference
        ChatApp chatApp = llmReq.getChatAppConfig().get(APP_KEY);
        ChatLanguageModel chatLanguageModel = getChatLanguageModel(chatApp.getChatModelConfig());
        SemanticSqlExtractor extractor =
                AiServices.create(SemanticSqlExtractor.class, chatLanguageModel);

        Map<Prompt, List<Text2SQLExemplar>> prompt2Exemplar = new HashMap<>();
        for (List<Text2SQLExemplar> exemplars : exemplarsList) {
            llmReq.setDynamicExemplars(exemplars);
            Prompt prompt = generatePrompt(llmReq, llmResp, chatApp);
            prompt2Exemplar.put(prompt, exemplars);
        }

        // 3.perform multiple self-consistency inferences parallelly
        Map<String, Prompt> output2Prompt = new ConcurrentHashMap<>();
        prompt2Exemplar.keySet().parallelStream().forEach(prompt -> {
            SemanticSql s2Sql = extractor.generateSemanticSql(prompt.toUserMessage().singleText());
            output2Prompt.put(s2Sql.getSql(), prompt);
            keyPipelineLog.info("OnePassSCSqlGenStrategy modelReq:\n{} \nmodelResp:\n{}",
                    prompt.text(), s2Sql);
        });

        // 4.format response.
        Pair<String, Map<String, Double>> sqlMapPair =
                ResponseHelper.selfConsistencyVote(Lists.newArrayList(output2Prompt.keySet()));
        llmResp.setSqlOutput(sqlMapPair.getLeft());
        List<Text2SQLExemplar> usedExemplars =
                prompt2Exemplar.get(output2Prompt.get(sqlMapPair.getLeft()));
        llmResp.setSqlRespMap(ResponseHelper.buildSqlRespMap(usedExemplars, sqlMapPair.getRight()));

        return llmResp;
    }

    private Prompt generatePrompt(LLMReq llmReq, LLMResp llmResp, ChatApp chatApp) {
        StringBuilder exemplars = new StringBuilder();
        for (Text2SQLExemplar exemplar : llmReq.getDynamicExemplars()) {
            String exemplarStr = String.format("\nQuestion:%s,Schema:%s,SideInfo:%s,SQL:%s",
                    exemplar.getQuestion(), exemplar.getDbSchema(), exemplar.getSideInfo(),
                    exemplar.getSql());
            exemplars.append(exemplarStr);
        }
        String dataSemantics = promptHelper.buildSchemaStr(llmReq);
        String sideInformation = promptHelper.buildSideInformation(llmReq);
        llmResp.setSchema(dataSemantics);
        llmResp.setSideInfo(sideInformation);

        Map<String, Object> variable = new HashMap<>();
        variable.put("exemplar", exemplars);
        variable.put("question", llmReq.getQueryText());
        variable.put("schema", dataSemantics);
        variable.put("information", sideInformation);

        // use custom prompt template if provided.
        String promptTemplate = chatApp.getPrompt();
        return PromptTemplate.from(promptTemplate).apply(variable);
    }

    @Override
    public void afterPropertiesSet() {
        SqlGenStrategyFactory
                .addSqlGenerationForFactory(LLMReq.SqlGenType.ONE_PASS_SELF_CONSISTENCY, this);
    }
}
