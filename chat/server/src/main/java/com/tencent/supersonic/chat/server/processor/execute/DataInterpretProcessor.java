package com.tencent.supersonic.chat.server.processor.execute;

import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.provider.DeepSeekModelFactory;
import dev.langchain4j.provider.ModelProvider;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * DataInterpretProcessor interprets query result to make it more readable to the users.
 */
@LiteflowComponent(DataInterpretProcessor.NODE_NAME)
public class DataInterpretProcessor extends NodeComponent implements ExecuteResultProcessor {

    public static final String NODE_NAME = "DataInterpretProcessor";

    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");

    public static final String APP_KEY = "DATA_INTERPRETER";
    private static final String INSTRUCTION = ""
            + "#Role: You are a data expert who communicates with business users everyday."
            + "\n#Task: Your will be provided with a question asked by a user and the relevant "
            + "result data queried from the databases, please interpret the data and organize a brief answer."
            + "\n#Rules: " + "\n1.ALWAYS respond in the use the same language as the `#Question`."
            + "\n2.ALWAYS reference some key data in the `#Answer`."
            + "\n#Question:{{question}} #Data:{{data}} #Answer:";

    public DataInterpretProcessor() {
        ChatAppManager.register(APP_KEY,
                ChatApp.builder().prompt(INSTRUCTION).name("结果数据解读").appModule(AppModule.CHAT)
                        .description("通过大模型对结果数据做提炼总结").enable(false)
                        .providerPrompts(Map.of(DeepSeekModelFactory.PROVIDER, """
                                #角色: 你是一名每天与业务用户沟通的数据专家。
                                #任务: 你将收到用户提出的问题以及从数据库中查询到的相关结果数据，请解读数据并组织简短回答。
                                #规则:
                                1.始终使用和`问题`相同的语言进行解读。
                                2.始终在`回答`中引用一些关键数据。
                                #问题:{{question}}
                                #数据:{{data}}
                                #回答:
                                """)).build());
    }

    @Override
    public void process() throws Exception {
        ExecuteContext executeContext = this.getContextBean(ExecuteContext.class);
        if(accept(executeContext)) {
            process(executeContext);
        }
    }


    @Override
    public boolean accept(ExecuteContext executeContext) {
        Agent agent = executeContext.getAgent();
        ChatApp chatApp = agent.getChatAppConfig().get(APP_KEY);
        return Objects.nonNull(chatApp) && chatApp.isEnable();
    }

    @Override
    public void process(ExecuteContext executeContext) {
        QueryResult queryResult = executeContext.getResponse();
        Agent agent = executeContext.getAgent();
        ChatApp chatApp = agent.getChatAppConfig().get(APP_KEY);

        Map<String, Object> variable = new HashMap<>();
        variable.put("question", executeContext.getRequest().getQueryText());
        variable.put("data", queryResult.getTextResult());

        Prompt prompt = PromptTemplate.from(chatApp.getPrompt()).apply(variable);
        ChatLanguageModel chatLanguageModel =
                ModelProvider.getChatModel(chatApp.getChatModelConfig());
        Response<AiMessage> response = chatLanguageModel.generate(prompt.toUserMessage());
        String anwser = response.content().text();
        keyPipelineLog.info("DataInterpretProcessor modelReq:\n{} \nmodelResp:\n{}", prompt.text(),
                anwser);
        if (StringUtils.isNotBlank(anwser)) {
            queryResult.setTextSummary(anwser);
        }
    }
}
