package com.custom.kafka.sample.payment;

import com.custom.kafka.common.message.KafkaEventMessage;
import com.custom.kafka.common.processor.KafkaMessageHandler;
import com.custom.kafka.sample.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConsumer {
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "payment-events",
            groupId = "${kafka.consumer.payment.group-id:kafka-sample-payment-group}",
            containerFactory = "paymentKafkaListenerContainerFactory"
    )
    @KafkaMessageHandler
    public void consume(ConsumerRecord<String, String> record) {
        PaymentEvent event = objectMapper.readValue(
                record.value(),
                objectMapper.getTypeFactory().constructParametricType(KafkaEventMessage.class, PaymentEvent.class)
        );
        log.info("결제 이벤트 처리: orderId={}, userId={}, amount={}, status={}",
                event.orderId(), event.userId(), event.amount(), event.status());

        switch (event.status()) {
            case COMPLETED -> processCompletedPayment(event);
            case CANCELLED -> processCancelledPayment(event);
            default -> log.warn("미처리 결제 상태: status={}", event.status());
        }
    }

    private void processCompletedPayment(PaymentEvent event) {
        log.info("결제 완료 처리: orderId={}, amount={}", event.orderId(), event.amount());
    }

    private void processCancelledPayment(PaymentEvent event) {
        log.info("결제 취소 처리: orderId={}", event.orderId());
    }
}
