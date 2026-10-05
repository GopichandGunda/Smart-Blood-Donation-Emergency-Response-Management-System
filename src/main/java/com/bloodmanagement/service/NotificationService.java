package com.bloodmanagement.service;

import com.bloodmanagement.dao.NotificationDAO;
import com.bloodmanagement.model.Notification;
import java.util.List;

public final class NotificationService {
    private final NotificationDAO notifications;

    public NotificationService(NotificationDAO notifications) {
        this.notifications = notifications;
    }

    public List<Notification> findForUser(long userId) {
        return notifications.findByUserId(userId);
    }

    public void markRead(long notificationId, long userId) {
        notifications.markRead(notificationId, userId);
    }
}
