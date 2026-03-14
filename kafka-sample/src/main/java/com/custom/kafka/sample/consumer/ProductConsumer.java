package com.custom.kafka.sample.consumer;

import com.custom.kafka.common.message.KafkaEventMessage;
import com.custom.kafka.common.processor.KafkaMessageHandler;
import com.custom.kafka.sample.product.ProductEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductConsumer {
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "product-events",
            groupId = "${kafka.consumer.product.group-id:kafka-sample-product-group}",
            containerFactory = "productKafkaListenerContainerFactory"
    )
    @KafkaMessageHandler
    public void consume(ConsumerRecord<String, String> record) {
        ProductEvent event = objectMapper.readValue(
                record.value(),
                objectMapper.getTypeFactory().constructParametricType(KafkaEventMessage.class, ProductEvent.class)
        );
        log.info("상품 이벤트 처리: productId={}, action={}, quantity={}",
                event.productId(), event.action(), event.quantity());

        switch (event.action()) {
            case STOCK_UPDATE -> processStockUpdate(event);
            case PRICE_CHANGE -> processPriceChange(event);
        }
    }

    private void processStockUpdate(ProductEvent event) {
        log.info("재고 업데이트: productId={}, quantity={}", event.productId(), event.quantity());
    }

    private void processPriceChange(ProductEvent event) {
        log.info("가격 변경: productId={}", event.productId());
    }
}
