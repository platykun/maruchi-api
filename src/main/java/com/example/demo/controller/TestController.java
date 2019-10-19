package com.example.demo.controller;

import com.example.demo.bean.TestBean;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.template.ConfirmTemplate;
import com.linecorp.bot.model.response.BotApiResponse;
import org.springframework.web.bind.annotation.*;

import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/test")
public class TestController {
    private final LineMessagingClient lineMessagingClient;

    TestController(LineMessagingClient lineMessagingClient) {
        this.lineMessagingClient = lineMessagingClient;
    }


//    @RequestMapping(method= RequestMethod.GET)
//    public TestBean get(@RequestParam(value="name") String name) {
//        return new TestBean("", "hello");
//    }


    //リマインドをプッシュ
    @RequestMapping(method = RequestMethod.GET)
    public void pushAlarm(
            @RequestParam(value = "shopId") String shopId,
            @RequestParam(value = "shopUrl") String shopUrl,
            @RequestParam(value = "shopName") String shopName
    ) throws URISyntaxException {

        try {
            BotApiResponse response = lineMessagingClient
                    // shimoe
//                    .pushMessage(new PushMessage("U53d1f385dce83150c8fe8b7659d65189",
                    .pushMessage(new PushMessage("U82279cb01a0eb054b4402341423f7678",
                            new TemplateMessage("ランチに行きましょう!",
                                    new ConfirmTemplate("ランチに行きましょう！\n\n" + shopName + "\n" + shopUrl,
                                            new MessageAction("行く", "行く"),
                                            new MessageAction("行かない", "行かない")
                                    )
                            )))
                    .get();
            System.out.println("Sent messages: {}" + response.toString());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
