package com.tencent.supersonic.chat.server.parser;

import com.tencent.supersonic.chat.server.plugin.recognize.PluginRecognizer;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.chat.server.util.ComponentFactory;
import com.tencent.supersonic.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;

@Slf4j
public class NL2PluginParser implements ChatQueryParser {

    private final List<PluginRecognizer> pluginRecognizers =
            ComponentFactory.getPluginRecognizers();

    @Override
    public String code() {
        return "functionCall";
    }

    @Override
    public String name() {
        return "外部调用";
    }

    @Override
    public String description() {
        return "调用外部能力,通过 iframe 方式嵌入";
    }

    @Override
    public Collection<String> keywords() {
        return List.of("数艺");
    }

    @Override
    public Collection<String> exemplars() {
        return List.of("打开数艺的仪表板");
    }

    @Override
    public boolean accept(ParseContext parseContext) {
        return parseContext.getAgent().containsPluginTool();
    }

    @Override
    public void parse(ParseContext parseContext) {
        pluginRecognizers.forEach(pluginRecognizer -> {
            pluginRecognizer.recognize(parseContext);
            log.info("{} recallResult:{}", pluginRecognizer.getClass().getSimpleName(),
                    JsonUtil.toString(parseContext.getResponse()));
        });
    }
}
