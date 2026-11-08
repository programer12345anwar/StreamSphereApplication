package com.streamsphere.central.event.producer;

import com.streamsphere.central.entity.VideoProcessingOutbox;
import com.streamsphere.video.events.VideoThumbnailRequestedEvent;
import com.streamsphere.central.repository.VideoProcessingOutboxRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class VideoProcessingOutboxPublisher {
    private final VideoProcessingOutboxRepo outboxRepo;
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public VideoProcessingOutboxPublisher(
            VideoProcessingOutboxRepo outboxRepo,
            RabbitTemplate rabbitTemplate,
            @Value("${thumbnail.events.exchange}") String exchange,
            @Value("${thumbnail.events.request-routing-key}") String routingKey) {
        this.outboxRepo = outboxRepo;
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Scheduled(fixedDelayString = "${thumbnail.outbox.poll-interval-ms:5000}")
    @Transactional
    public void publishPendingEvents() {
        for (VideoProcessingOutbox row : outboxRepo.findTop25ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            CorrelationData correlation = new CorrelationData(row.getId().toString());
            try {
                rabbitTemplate.convertAndSend(
                        exchange,
                        routingKey,
                        new VideoThumbnailRequestedEvent(
                                row.getId(), row.getVideoId(), row.getVideoUrl(), row.getCustomThumbnailUrl()),
                        correlation);
                CorrelationData.Confirm confirm = correlation.getFuture().get(10, TimeUnit.SECONDS);
                if (!confirm.isAck() || correlation.getReturned() != null) {
                    throw new IllegalStateException("RabbitMQ did not confirm the thumbnail event.");
                }
                row.setPublishedAt(LocalDateTime.now());
                outboxRepo.save(row);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while publishing thumbnail event {}", row.getId(), ex);
                return;
            } catch (Exception ex) {
                log.error("Could not publish thumbnail event {}; it will be retried", row.getId(), ex);
                return;
            }
        }
    }
}
