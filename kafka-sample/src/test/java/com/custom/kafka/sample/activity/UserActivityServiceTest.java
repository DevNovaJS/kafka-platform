package com.custom.kafka.sample.activity;

import com.mongodb.client.result.UpdateResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private UserActivityService userActivityService;

    @Test
    void process_upsertsActivityLogAndStats() {
        // given
        UserActivityEvent event = new UserActivityEvent(
                "user-1", "sess-1", ActivityType.PAGE_VIEW, "page-home", Map.of("ref", "google")
        );
        when(mongoTemplate.upsert(any(Query.class), any(Update.class), eq(UserActivityLog.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        when(mongoTemplate.upsert(any(Query.class), any(Update.class), eq(UserActivityStats.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));

        // when
        userActivityService.process(event, "msg-1");

        // then
        verify(mongoTemplate).upsert(any(Query.class), any(Update.class), eq(UserActivityLog.class));
        verify(mongoTemplate).upsert(any(Query.class), any(Update.class), eq(UserActivityStats.class));
    }

    @Test
    void process_logUpsert_usesMessageIdAsCriteria() {
        // given
        UserActivityEvent event = new UserActivityEvent(
                "user-2", "sess-2", ActivityType.CLICK, "btn-buy", null
        );
        when(mongoTemplate.upsert(any(Query.class), any(Update.class), any(Class.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));

        // when
        userActivityService.process(event, "msg-unique");

        // then — first upsert call is for UserActivityLog with messageId criteria
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate, times(2)).upsert(queryCaptor.capture(), any(Update.class), any(Class.class));

        Query logQuery = queryCaptor.getAllValues().getFirst();
        assertThat(logQuery.toString()).contains("messageId");
        assertThat(logQuery.toString()).contains("msg-unique");
    }

    @Test
    void process_statsUpsert_usesActivityTypeAndTargetIdAsCriteria() {
        // given
        UserActivityEvent event = new UserActivityEvent(
                "user-3", "sess-3", ActivityType.SEARCH, "query-java", null
        );
        when(mongoTemplate.upsert(any(Query.class), any(Update.class), any(Class.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));

        // when
        userActivityService.process(event, "msg-3");

        // then — second upsert call is for UserActivityStats
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate, times(2)).upsert(queryCaptor.capture(), any(Update.class), any(Class.class));

        Query statsQuery = queryCaptor.getAllValues().get(1);
        assertThat(statsQuery.toString()).contains("activityType");
        assertThat(statsQuery.toString()).contains("targetId");
    }
}
