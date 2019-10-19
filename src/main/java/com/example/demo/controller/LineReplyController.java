package com.example.demo.controller;

import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;

@LineMessageHandler
public class LineReplyController {


    // ユーザからの問い合わせに返信する
    @EventMapping
    public TextMessage handleTextMessageEvent(MessageEvent<TextMessageContent> event) throws Exception {
        try {
            System.out.println("event: " + event.toString());
            System.out.println("source: " + event.getSource());
            System.out.println("source.tostring: " + event.getSource().toString());
            System.out.println("source.tostring: " + event.getSource().getClass().toString());
            System.out.println("source.senderId: " + event.getSource().getSenderId());

            String id = event.getMessage().getId();
            String text = event.getMessage().getText();
            System.out.println("id: " + id);
            System.out.println("text: " + text);

            if("Agree!".equals(text)) {
                return new TextMessage("OK!");
            } else if("Not now.".equals(text)) {
                return new TextMessage("😢");
            }

            return new TextMessage("");
        } catch (Exception e) {
            // エラーは握りつぶす
            return new TextMessage("OK!");
        }
    }
}
