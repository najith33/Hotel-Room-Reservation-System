package com.hotel.roomreservation.strategy;

import com.hotel.roomreservation.model.Notification;
import com.hotel.roomreservation.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("inAppNotificationStrategy")
public class InAppNotificationStrategy implements NotificationStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(InAppNotificationStrategy.class);
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Override
    public boolean sendNotification(Notification notification) {
        try {
            // Save notification to database for in-app display
            notificationRepository.save(notification);
            logger.info("In-app notification saved successfully for user: {}", notification.getRecipientUser().getUsername());
            return true;
        } catch (Exception e) {
            logger.error("Failed to save in-app notification for user: {}", notification.getRecipientUser().getUsername(), e);
            return false;
        }
    }
    
    @Override
    public String getStrategyType() {
        return "IN_APP";
    }
}
