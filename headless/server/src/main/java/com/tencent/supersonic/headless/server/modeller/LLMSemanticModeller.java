package com.tencent.supersonic.headless.server.modeller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.supersonic.common.config.ChatModel;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.service.ChatModelService;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.DbSchema;
import com.tencent.supersonic.headless.api.pojo.ModelSchema;
import com.tencent.supersonic.headless.api.pojo.request.ModelBuildReq;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.provider.DeepSeekModelFactory;
import dev.langchain4j.provider.ModelProvider;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class LLMSemanticModeller implements SemanticModeller {

    public static final String APP_KEY = "BUILD_DATA_MODEL";

    private static final String SYS_EXEMPLAR_FILE = "s2-buildModel-exemplar.json";

    public static final String INSTRUCTION = ""
            + "Role: As an experienced data analyst with extensive modeling experience, "
            + "      you are expected to have a deep understanding of data analysis and data modeling concepts."
            + "\nJob: You will be given a database table structure, which includes the database table name, field name,"
            + "       field type, and field comments. Your task is to utilize this information for data modeling."
            + "\nTask:"
            + "\n1. Generate a name and description for the model. Please note, 'bizName' refers to the English name, while 'name' is the Chinese name."
            + "\n2. Create a Chinese name for the field and categorize the field into one of the following five types:"
            + "\n   primary_key: This is a unique identifier for a record row in a database."
            + "\n   foreign_key: This is a key in a database whose value is derived from the primary key of another table."
            + "\n   partition_time: This represents the time when data is generated in the data warehouse."
            + "\n   dimension: Usually a string type, used for grouping and filtering data. No need to generate aggregate functions"
            + "\n   measure: Usually a numeric type, used to quantify data from a certain evaluative perspective. "
            + "              Also, you need to generate aggregate functions(Eg: MAX, MIN, AVG, SUM, COUNT) for the measure type. "
            + "\nTip: I will also give you other related dbSchemas. If you determine that different dbSchemas have the same fields, "
            + "       they can be primary and foreign key relationships."
            + "\nDBSchema: {{DBSchema}}" + "\nOtherRelatedDBSchema: {{otherRelatedDBSchema}}"
            + "\nExemplar: {{exemplar}}";

    private final ObjectMapper objectMapper = JsonUtil.INSTANCE.getObjectMapper();

    public LLMSemanticModeller() {
        ChatAppManager.register(APP_KEY,
                ChatApp.builder().prompt(INSTRUCTION).name("构造数据语义模型").appModule(AppModule.HEADLESS)
                        .description("通过大模型来构造数据语义模型").enable(true)
                        .providerPrompts(Map.of(DeepSeekModelFactory.PROVIDER,
                                """
                                        角色: 作为一名经验丰富且具备深厚建模背景的数据分析师，您需要对数据分析和数据建模有深刻的理解。您的任务是基于给定的数据库表结构信息来构建数据模型。

                                        ### 背景信息
                                        - 您将收到一个或多个数据库表结构描述，每个描述包括表名、字段名称、字段类型及字段注释。
                                        - `bizName`代表英文名称，而`Name`则指中文名称。
                                        - 如果提供了多个表结构映射（通过{{otherRelatedDBSchema}}变量），请注意识别可能存在的主键与外键关系。

                                        ### 任务要求
                                        1. **生成模型名称与描述**：根据提供的表结构信息，为每个模型创建一个合适的名称和描述，并简要描述该模型的作用。
                                        2. **字段处理**：
                                           - 为每个字段提供一个中文名称。
                                           - 将每个字段归类到以下五种类型之一：
                                             - `primary_key`: 表示记录行在数据库中的唯一标识符。
                                             - `foreign_key`: 字段值来源于另一个表的主键。
                                             - `partition_time`: 用于表示数据仓库中数据生成的时间点。
                                             - `dimension`: 通常为字符串类型，适用于数据分组和过滤操作。对于此类别，请不要指定聚合函数。
                                             - `measure`: 代表数值型指标，用来量化特定方面的数据表现。对于每项度量指标，请同时定义其适用的聚合函数（例如：MAX, MIN, AVG, COUNT, SUM）。
                                        3. **附加说明**：如果发现不同表之间存在相同命名的字段，则这些字段很可能构成了主键-外键的关系，请据此调整您的分类决策。

                                        ### 输入格式
                                        - 数据库表结构: {{DBSchema}}
                                        - 其他相关表结构: {{otherRelatedDBSchema}}

                                        ### 输出格式
                                        请按照上述指示组织您的答案，并确保所有内容都清晰易懂。如果有任何假设或者特别考虑的地方，请在文档中明确指出。

                                        ### 示例
                                        {{exemplar}}

                                        """))
                        .build());
    }

    interface ModelSchemaExtractor {
        ModelSchema generateModelSchema(String text);
    }

    @Override
    public void build(DbSchema dbSchema, List<DbSchema> dbSchemas, ModelSchema modelSchema,
            ModelBuildReq modelBuildReq) {
        if (!modelBuildReq.isBuildByLLM()) {
            return;
        }
        Optional<ChatApp> chatApp = ChatAppManager.getApp(APP_KEY);
        if (!chatApp.isPresent() || !chatApp.get().isEnable()) {
            return;
        }
        List<DbSchema> otherDbSchema = getOtherDbSchema(dbSchema, dbSchemas);
        ModelSchemaExtractor extractor =
                AiServices.create(ModelSchemaExtractor.class, getChatModel(modelBuildReq));
        Prompt prompt = generatePrompt(dbSchema, otherDbSchema, chatApp.get());
        modelSchema = extractor.generateModelSchema(prompt.toUserMessage().singleText());
        log.info("dbSchema:  {}\n otherRelatedDBSchema:{}\n modelSchema: {}",
                JsonUtil.toString(dbSchema), JsonUtil.toString(otherDbSchema),
                JsonUtil.toString(modelSchema));
    }

    private List<DbSchema> getOtherDbSchema(DbSchema curSchema, List<DbSchema> dbSchemas) {
        return dbSchemas.stream()
                .filter(dbSchema -> !dbSchema.getTable().equals(curSchema.getTable()))
                .collect(Collectors.toList());
    }

    private ChatLanguageModel getChatModel(ModelBuildReq modelBuildReq) {
        ChatModelConfig chatModelConfig = modelBuildReq.getChatModelConfig();
        if (chatModelConfig == null) {
            ChatModelService chatModelService = ContextUtils.getBean(ChatModelService.class);
            ChatModel chatModel = chatModelService.getChatModel(modelBuildReq.getChatModelId());
            chatModelConfig = chatModel.getConfig();
        }
        return ModelProvider.getChatModel(chatModelConfig);
    }

    private Prompt generatePrompt(DbSchema dbSchema, List<DbSchema> otherDbSchema,
            ChatApp chatApp) {
        Map<String, Object> variable = new HashMap<>();
        variable.put("exemplar", loadExemplars());
        variable.put("DBSchema", JsonUtil.toString(dbSchema));
        variable.put("otherRelatedDBSchema", JsonUtil.toString(otherDbSchema));
        return PromptTemplate.from(chatApp.getPrompt()).apply(variable);
    }

    private String loadExemplars() {
        Environment environment = ContextUtils.getBean(Environment.class);
        String enableExemplarLoading =
                environment.getProperty("s2.model.building.exemplars.enabled");
        if (Boolean.FALSE.equals(Boolean.parseBoolean(enableExemplarLoading))) {
            log.info("Not enable load model-building exemplars");
            return "";
        }
        try {
            ClassPathResource resource = new ClassPathResource(SYS_EXEMPLAR_FILE);
            if (resource.exists()) {
                InputStream inputStream = resource.getInputStream();
                return objectMapper
                        .writeValueAsString(objectMapper.readValue(inputStream, Object.class));
            }
        } catch (Exception e) {
            log.error("Failed to load model-building system exemplars", e);
        }
        return "";
    }

}
