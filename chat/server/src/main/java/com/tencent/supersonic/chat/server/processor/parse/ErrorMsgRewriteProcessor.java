package com.tencent.supersonic.chat.server.processor.parse;

import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.server.utils.ModelConfigHelper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.provider.DeepSeekModelFactory;
import dev.langchain4j.provider.ModelProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * ErrorMsgRewriteProcessor rewrites error message to make it more readable to the users.
 **/
public class ErrorMsgRewriteProcessor implements ParseResultProcessor {

    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");

    public static final String APP_KEY = "REWRITE_ERROR_MESSAGE";
    private static final String REWRITE_ERROR_MESSAGE_INSTRUCTION = ""
            + "#Role: You are a data business partner who closely interacts with business people.\n"
            + "#Task: Your will be provided with user input, system output and some examples, "
            + "please respond shortly to teach user how to ask the right question, "
            + "by using `Examples` as references."
            + "#Rules: ALWAYS respond with the same language as the `Input`.\n"
            + "#Input: {{user_question}}\n" + "#Output: {{system_message}}\n"
            + "#Examples: {{examples}}\n" + "#Response: ";

    public ErrorMsgRewriteProcessor() {
        ChatAppManager.register(APP_KEY,
                ChatApp.builder().prompt(REWRITE_ERROR_MESSAGE_INSTRUCTION).name("异常提示改写")
                        .appModule(AppModule.CHAT).description("通过大模型将异常信息改写为更友好和引导性的提示用语")
                        .enable(true)
                        .providerPrompts(Map.of(DeepSeekModelFactory.PROVIDER,
                                """
                                        # 角色：作为与业务人员紧密合作的数据业务合作伙伴。
                                        # 任务：基于用户提供的输入、系统输出以及示例，指导用户如何利用这些示例来提出更准确的问题。
                                        # 规则：
                                        1. 回复时，请确保使用与用户问题相同的语言。
                                        2. 结构化你的回答，使其易于理解且具有指导性。

                                        # 用户输入: {{user_question}}
                                        # 系统输出: {{system_message}}
                                        # 示例: {{examples}}

                                        # 回应指南：

                                        1. **分析用户问题**：首先明确用户想要了解的具体内容或遇到的问题是什么。
                                        2. **参考系统输出**：查看系统给出的信息是否已经部分解答了用户的疑问；如果答案不完全，则需进一步引导。
                                        3. **利用示例**：通过分析给定的示例，展示如何根据具体情况构建有效的问题。解释每个示例是如何帮助获取所需信息的，并指出其中的关键点。
                                        4. **提供模板**：基于上述步骤，为用户提供一个或多个提问模板，以便他们能够更好地表述自己的需求。例如：“为了让我们更清楚地了解您的需求，请尝试这样描述您的问题：[具体问题] + [期望得到的答案类型]。”
                                        """))
                        .build());
    }

    @Override
    public boolean accept(ParseContext parseContext) {
        ChatApp chatApp = parseContext.getAgent().getChatAppConfig().get(APP_KEY);
        return StringUtils.isNotBlank(parseContext.getResponse().getErrorMsg())
                && Objects.nonNull(chatApp) && chatApp.isEnable();
    }

    @Override
    public void process(ParseContext parseContext) {
        String errMsg = parseContext.getResponse().getErrorMsg();
        ChatApp chatApp = parseContext.getAgent().getChatAppConfig().get(APP_KEY);
        Map<String, Object> variables = new HashMap<>();
        variables.put("user_question", parseContext.getRequest().getQueryText());
        variables.put("system_message", errMsg);

        StringBuilder exampleStr = new StringBuilder();
        if (parseContext.getResponse().getUsedExemplars() != null) {
            parseContext.getResponse().getUsedExemplars().forEach(e -> exampleStr.append(String
                    .format("<Question:{%s},Schema:{%s}> ", e.getQuestion(), e.getDbSchema())));
        }
        if (parseContext.getAgent().getExamples() != null) {
            parseContext.getAgent().getExamples()
                    .forEach(e -> exampleStr.append(String.format("<Question:{%s}> ", e)));
        }
        variables.put("examples", exampleStr);

        Prompt prompt = PromptTemplate.from(chatApp.getPrompt()).apply(variables);
        ChatLanguageModel chatLanguageModel =
                ModelProvider.getChatModel(ModelConfigHelper.getChatModelConfig(chatApp));
        Response<AiMessage> response = chatLanguageModel.generate(prompt.toUserMessage());
        String rewrittenMsg = response.content().text();
        parseContext.getResponse().setErrorMsg(rewrittenMsg);
        parseContext.getResponse().setState(ParseResp.ParseState.FAILED);
        keyPipelineLog.info("ErrorMessageProcessor modelReq:\n{} \nmodelResp:\n{}", prompt.text(),
                rewrittenMsg);
    }

}
