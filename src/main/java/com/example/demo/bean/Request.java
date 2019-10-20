package com.example.demo.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Request {
    private List<LineEvent> events;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LineEvent {
        private String type;
        private String replyToken;
        private Source source;
        private long timestamp;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Source {
        private String groupId;
        private String type;
    }
}
