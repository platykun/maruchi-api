package com.example.demo.controller;

import com.cybozu.kintone.client.authentication.Auth;
import com.cybozu.kintone.client.connection.Connection;
import com.cybozu.kintone.client.exception.KintoneAPIException;
import com.cybozu.kintone.client.model.record.GetRecordsResponse;
import com.cybozu.kintone.client.module.record.Record;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.template.ButtonsTemplate;
import com.linecorp.bot.model.response.BotApiResponse;
import java.util.stream.Collectors;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestOperations;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
public class TestController {

    private final LineMessagingClient lineMessagingClient;
    private final RestOperations restOperations;
    private static final DateTimeFormatter MEETING_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    private final String G_NAVI_END_POINT = "https://api.gnavi.co.jp/RestSearchAPI/v3/";
    private final String apiKey;
    private final String apiToken;
    private final String kintoneDomain;
    private static final Integer APP_ID = 4;

    @Autowired
    TestController(LineMessagingClient lineMessagingClient,
                   RestTemplateBuilder restTemplateBuilder,
                   @Value("${gnavi.api.key}") String apiKey,
                   @Value("${kintone.api.token}") String apiToken,
                   @Value("${kintone.domain}") String kintoneDomain) {
        this.lineMessagingClient = lineMessagingClient;
        this.restOperations = restTemplateBuilder.build();
        this.apiKey = apiKey;
        this.apiToken = apiToken;
        this.kintoneDomain = kintoneDomain;
    }

    //リマインドをプッシュ
    @RequestMapping(path = "/test", method = RequestMethod.GET)
    public void pushAlarm(
            @RequestParam(value = "shopId") String shopId,
            @RequestParam(value = "shopUrl") String shopUrl,
            @RequestParam(value = "shopName") String shopName
    ) throws URISyntaxException {

        try {

            ZonedDateTime japanTime = ZonedDateTime.now(ZoneId.of("Japan"));
            String meetingTime = MEETING_TIME_FORMATTER.format(japanTime.plusMinutes(15));

            URI uri = URI.create(G_NAVI_END_POINT + "?keyid=" + apiKey + "&id=" + shopId);
            ResponseEntity<Response> res = restOperations.getForEntity(uri, Response.class);
            final String imageUri;
            if (res.getStatusCode() == HttpStatus.OK) {
                Response response = res.getBody();
                imageUri =
                        response.getRest().stream()
                                .filter(r -> r.getId().equals(shopId))
                                .map(r -> URI.create(r.getImageUrl().getShopImage1()).toString())
                                .findFirst()
                                .orElse(null);
            } else {
                imageUri = null;
            }

            Auth kintoneAuth = new Auth();
            kintoneAuth.setApiToken(apiToken);

            Connection kintoneConnection = new Connection(kintoneDomain, kintoneAuth);
            Record kintoneRecord = new Record(kintoneConnection);

            GetRecordsResponse records = kintoneRecord.getRecords(APP_ID, null, null, null);
            List<String> groupIds =
                records.getRecords().stream()
                    .map(m -> m.get("groupId").getValue().toString())
                    .collect(Collectors.toList());

            groupIds.stream().forEach(groupId -> {
                BotApiResponse response = null;
                try {
                    response = lineMessagingClient
                        .pushMessage(
                            new PushMessage(
                                groupId,
                                new TemplateMessage("ランチに行きましょう!",
                                    new ButtonsTemplate(
                                        imageUri,
                                        shopName,
                                        String.format("Let's go to lunch！\n %s に 1 階ロビー集合！", meetingTime),
                                        Arrays.asList(
                                            new MessageAction("Agree!", "Agree!"),
                                            new MessageAction("Not now.", "Not now."))))))
                        .get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("Sent messages: {}" + response.toString());
            });
        } catch (KintoneAPIException e) {
            throw new RuntimeException(e);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {

        private List<Rest> rest;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Rest {
            private String id;
            private Image imageUrl;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Image {
            private String shopImage1;
        }
    }
}
