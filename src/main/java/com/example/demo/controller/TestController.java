package com.example.demo.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.template.ButtonsTemplate;
import com.linecorp.bot.model.response.BotApiResponse;
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

    @Autowired
    TestController(LineMessagingClient lineMessagingClient,
                   RestTemplateBuilder restTemplateBuilder,
                   @Value("${gnavi.api.key}") String apiKey) {
        this.lineMessagingClient = lineMessagingClient;
        this.restOperations = restTemplateBuilder.build();
        this.apiKey = apiKey;
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
            String imageUri = null;
            if (res.getStatusCode() == HttpStatus.OK) {
                Response response = res.getBody();
                imageUri =
                        response.getRest().stream()
                                .filter(r -> r.getId().equals(shopId))
                                .map(r -> URI.create(r.getImageUrl().getShopImage1()).toString())
                                .findFirst()
                                .orElse(null);
            }

            BotApiResponse response = lineMessagingClient
                    // shimoe
                    //                    .pushMessage(new
                    // PushMessage("U53d1f385dce83150c8fe8b7659d65189",
                    //                    .pushMessage(new
                    .pushMessage(
                            new PushMessage(
                                    "C71c4154140fbf5a629d2f3eced527937",
                                    new TemplateMessage("ランチに行きましょう!",
                                            new ButtonsTemplate(
                                                    imageUri,
                                                    shopName,
                                                    String.format("Let's go to lunch！\n %s に 1 階ロビー集合！", meetingTime),
                                                    Arrays.asList(
                                                            new MessageAction("Agree!", "Agree!"),
                                                            new MessageAction("Not now.", "Not now."))))))
                    .get();
            System.out.println("Sent messages: {}" + response.toString());
        } catch (InterruptedException | ExecutionException e) {
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
