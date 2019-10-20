package com.example.demo.controller;

import com.cybozu.kintone.client.authentication.Auth;
import com.cybozu.kintone.client.connection.Connection;
import com.cybozu.kintone.client.exception.KintoneAPIException;
import com.cybozu.kintone.client.model.app.form.FieldType;
import com.cybozu.kintone.client.model.record.AddRecordResponse;
import com.cybozu.kintone.client.model.record.GetRecordsResponse;
import com.cybozu.kintone.client.model.record.UpdateRecordResponse;
import com.cybozu.kintone.client.model.record.field.FieldValue;
import com.cybozu.kintone.client.module.record.Record;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.event.source.GroupSource;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import java.util.HashMap;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

@LineMessageHandler
public class LineReplyController {

    private final String apiToken;
    private final String kintoneDomain;
    private static final Integer APP_ID = 4;

    @Autowired
    LineReplyController(
        @Value("${kintone.api.token}") String apiToken,
        @Value("${kintone.domain}") String kintoneDomain) {
        this.apiToken = apiToken;
        this.kintoneDomain = kintoneDomain;
    }

    @EventMapping
    public TextMessage reply(MessageEvent<TextMessageContent> context) {

        String userId = context.getSource().getUserId();
        try {

            if (context.getSource() instanceof GroupSource) {
                GroupSource source = (GroupSource) context.getSource();
                if("Agree!".equals(context.getMessage().getText())) {
                    return new TextMessage("OK!");
                } else if("Not now.".equals(context.getMessage().getText())) {
                    return new TextMessage("😢");
                } else {
                    Auth kintoneAuth = new Auth();
                    kintoneAuth.setApiToken(apiToken);

                    Connection kintoneConnection = new Connection(kintoneDomain, kintoneAuth);

                    Record kintoneRecord = new Record(kintoneConnection);

                    try {
                        HashMap<String, FieldValue> data = new HashMap<>();
                        FieldValue key = new FieldValue();
                        key.setType(FieldType.SINGLE_LINE_TEXT);
                        key.setValue(userId);
                        data.put("id", key);

                        FieldValue replyToken = new FieldValue();
                        replyToken.setType(FieldType.SINGLE_LINE_TEXT);
                        replyToken.setValue(context.getReplyToken());
                        data.put("replyToken", replyToken);

                        FieldValue groupId = new FieldValue();
                        groupId.setType(FieldType.SINGLE_LINE_TEXT);
                        groupId.setValue(source.getGroupId());
                        data.put("groupId", groupId);

                        GetRecordsResponse records = kintoneRecord.getRecords(APP_ID, null, null, null);
                        Optional<HashMap<String, FieldValue>> record =
                            records.getRecords().stream()
                                .filter(m -> m.get("id").getValue().equals(userId))
                                .findFirst();

                        if (record.isPresent()) {
                            UpdateRecordResponse response =
                                kintoneRecord.updateRecordByID(
                                    APP_ID,
                                    Integer.parseInt(record.get().get("$id").getValue().toString()),
                                    data,
                                    Integer.parseInt(record.get().get("$revision").getValue().toString()));
                            System.out.println("rev:" + response.getRevision());
                        } else {
                            AddRecordResponse response = kintoneRecord.addRecord(APP_ID, data);
                            System.out.println(response.getID() + ":" + response.getRevision());
                        }
                        return new TextMessage("");
                    } catch (KintoneAPIException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return new TextMessage("");
        } catch (Exception e) {
            return new TextMessage("OK!");
        }
    }
}
