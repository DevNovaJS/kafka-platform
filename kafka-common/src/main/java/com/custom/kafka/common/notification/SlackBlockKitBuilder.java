package com.custom.kafka.common.notification;

import com.slack.api.model.block.HeaderBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.composition.TextObject;
import com.slack.api.util.json.GsonFactory;
import com.slack.api.webhook.Payload;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SlackBlockKitBuilder {
    private static final int MAX_TEXT_LENGTH = 2500;

    public static String build(String header, String[][] fields, String message, String stackTrace) {
        List<LayoutBlock> blocks = new ArrayList<>();

        blocks.add(HeaderBlock.builder()
                .text(PlainTextObject.builder().text(header).emoji(true).build())
                .build());

        List<TextObject> fieldList = new ArrayList<>();
        for (String[] field : fields) {
            fieldList.add(MarkdownTextObject.builder()
                    .text("*" + field[0] + ":*\n" + field[1])
                    .build());
        }
        blocks.add(SectionBlock.builder().fields(fieldList).build());

        if (message != null && !message.isBlank()) {
            blocks.add(SectionBlock.builder()
                    .text(MarkdownTextObject.builder()
                            .text("*Message:*\n```" + truncate(message) + "```")
                            .build())
                    .build());
        }

        if (stackTrace != null && !stackTrace.isBlank()) {
            blocks.add(SectionBlock.builder()
                    .text(MarkdownTextObject.builder()
                            .text("*Stacktrace:*\n```" + truncate(stackTrace) + "```")
                            .build())
                    .build());
        }

        Payload payload = Payload.builder().blocks(blocks).build();
        return GsonFactory.createSnakeCase().toJson(payload);
    }

    private static String truncate(String text) {
        if (text.length() <= MAX_TEXT_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_TEXT_LENGTH) + "…";
    }
}
