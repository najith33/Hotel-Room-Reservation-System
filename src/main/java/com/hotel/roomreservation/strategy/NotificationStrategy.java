package com.hotel.roomreservation.strategy;

import com.hotel.roomreservation.model.Notification;

public interface NotificationStrategy {
    
    /**
     * Send a notification using the specific strategy implementation
     * @param notification The notification to send
     * @return true if notification was sent successfully, false otherwise
     */
    boolean sendNotification(Notification notification);
    
    /**
     * Get the strategy type name
     * @return The strategy type identifier
     */
    String getStrategyType();
}
