package com.tencent.supersonic.chat.server.parser;

import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;

import java.util.Collection;
import java.util.List;

public class PlainTextParser implements ChatQueryParser {

    @Override
    public String code() {
        return "qa";
    }

    @Override
    public String name() {
        return "问答";
    }

    @Override
    public String description() {
        return "通用知识/实时信息/无需专业数据支持的问答";
    }

    @Override
    public Collection<String> keywords() {
        return List.of("是什么", "怎么样", "为什么", "如何", "解释", "建议");
    }

    @Override
    public Collection<String> exemplars() {
        return List.of("如何预防感冒?","推荐适合糖尿病患者的运动");
    }

    @Override
    public boolean accept(ParseContext parseContext) {
        return true;
    }

    @Override
    public void parse(ParseContext parseContext) {
        SemanticParseInfo parseInfo = new SemanticParseInfo();
        parseInfo.setQueryMode("PLAIN_TEXT");
        parseInfo.setId(1);
        parseContext.getResponse().getSelectedParses().add(parseInfo);
        parseContext.getResponse().setState(ParseResp.ParseState.COMPLETED);
    }
}
