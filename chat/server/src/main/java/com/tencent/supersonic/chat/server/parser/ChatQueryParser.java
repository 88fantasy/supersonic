package com.tencent.supersonic.chat.server.parser;

import com.tencent.supersonic.chat.server.pojo.ParseContext;

import java.util.Collection;

public interface ChatQueryParser {

    String code();

    String name();

    String description();

    Collection<String> keywords();

    Collection<String> exemplars();

    boolean accept(ParseContext parseContext);

    void parse(ParseContext parseContext);
}
