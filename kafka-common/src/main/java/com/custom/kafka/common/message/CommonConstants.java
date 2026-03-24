package com.custom.kafka.common.message;


import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommonConstants {
    public static final String DLT_TOPIC_SUFFIX = "-DLT";
    public static final DateTimeFormatter SLACK_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
}
