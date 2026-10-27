package com.streamsphere.notification.controller;

import com.streamsphere.notification.event.model.NotificationMessage;
import com.streamsphere.notification.service.CommonUserService;
import com.streamsphere.notification.service.DuplicateMessageGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CommonControllerTest {

    private CommonController controller;
    private CommonUserService commonUserService;
    private DuplicateMessageGuard duplicateMessageGuard;

    @BeforeEach
    void setUp() {
        controller = new CommonController();
        commonUserService = mock(CommonUserService.class);
        duplicateMessageGuard = new DuplicateMessageGuard();
        ReflectionTestUtils.setField(controller, "commonUserService", commonUserService);
        ReflectionTestUtils.setField(controller, "duplicateMessageGuard", duplicateMessageGuard);
    }

    @Test
    void failedRegistrationEmailIsRetriedInsteadOfAcknowledgedAndDeduplicated() throws Exception {
        NotificationMessage message = registrationMessage();
        doThrow(new IllegalStateException("SMTP unavailable"))
                .doNothing()
                .when(commonUserService).sendUserRegistrationEmail(message);

        assertThrows(IllegalStateException.class, () -> controller.consumeMessage(message));
        controller.consumeMessage(message);
        verify(commonUserService, times(2)).sendUserRegistrationEmail(message);
    }

    @Test
    void successfullySentRegistrationEmailIsNotSentAgainOnRedelivery() throws Exception {
        NotificationMessage message = registrationMessage();

        controller.consumeMessage(message);
        controller.consumeMessage(message);

        verify(commonUserService).sendUserRegistrationEmail(message);
    }

    @Test
    void channelEmailFailureIsPropagatedForBrokerRetry() throws Exception {
        NotificationMessage message = registrationMessage();
        message.setType("create_channel");
        doThrow(new IllegalStateException("SMTP unavailable"))
                .when(commonUserService).sendCreateChannelNotification(message);

        assertThrows(IllegalStateException.class, () -> controller.consumeMessage(message));

        verify(commonUserService).sendCreateChannelNotification(message);
    }

    private NotificationMessage registrationMessage() {
        NotificationMessage message = new NotificationMessage();
        message.setType("user_registration");
        message.setEmail("test@example.com");
        message.setName("Test User");
        return message;
    }
}
