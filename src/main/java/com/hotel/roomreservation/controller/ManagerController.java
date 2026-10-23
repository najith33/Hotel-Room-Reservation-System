package com.hotel.roomreservation.controller;

import com.hotel.roomreservation.model.Booking;
import com.hotel.roomreservation.model.User;
import com.hotel.roomreservation.model.Room;
import com.hotel.roomreservation.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/manager")
public class ManagerController {
    
    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private RoomService roomService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private com.hotel.roomreservation.service.AvailabilityService availabilityService;
    
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);
        
        // Get statistics
        List<Booking> allBookings = bookingService.getBookingsByDateRange(LocalDate.now().minusDays(30), LocalDate.now().plusDays(30));
        List<Booking> pendingBookings = bookingService.getBookingsByStatus(Booking.BookingStatus.PENDING);
        List<Booking> confirmedBookings = bookingService.getBookingsByStatus(Booking.BookingStatus.CONFIRMED);
        
        // Get recent notifications
        List<com.hotel.roomreservation.model.Notification> recentNotifications = 
                notificationService.getNotificationsForUser(user.getUserId()).stream().limit(5).toList();
        Long unreadCount = notificationService.getUnreadNotificationCount(user.getUserId());
        
        model.addAttribute("user", user);
        model.addAttribute("totalBookings", allBookings.size());
        model.addAttribute("pendingBookings", pendingBookings.size());
        model.addAttribute("confirmedBookings", confirmedBookings.size());
        model.addAttribute("recentBookings", allBookings.stream().limit(10).toList());
        model.addAttribute("recentNotifications", recentNotifications);
        model.addAttribute("unreadCount", unreadCount);
        
        return "manager/dashboard";
    }
    
    @GetMapping("/bookings")
    public String viewAllBookings(Model model) {
        List<Booking> bookings = bookingService.getBookingsByDateRange(LocalDate.now().minusDays(30), LocalDate.now().plusDays(30));
        model.addAttribute("bookings", bookings);
        return "manager/bookings";
    }
    
    @GetMapping("/bookings/pending")
    public String viewPendingBookings(Model model) {
        List<Booking> bookings = bookingService.getBookingsByStatus(Booking.BookingStatus.PENDING);
        model.addAttribute("bookings", bookings);
        return "manager/pending-bookings";
    }
    
    @PostMapping("/bookings/{bookingId}/confirm")
    public String confirmBooking(@PathVariable Long bookingId, RedirectAttributes redirectAttributes) {
        try {
            bookingService.confirmBooking(bookingId);
            redirectAttributes.addFlashAttribute("success", "Booking confirmed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/bookings/pending";
    }
    
    @PostMapping("/bookings/{bookingId}/cancel")
    public String cancelBooking(@PathVariable Long bookingId, RedirectAttributes redirectAttributes) {
        try {
            bookingService.cancelBooking(bookingId);
            redirectAttributes.addFlashAttribute("success", "Booking cancelled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/bookings";
    }
    
    @GetMapping("/staff")
    public String manageStaff(Model model) {
        List<User> staff = userService.getUsersByRole("GENERAL_MANAGER");
        staff.addAll(userService.getUsersByRole("RESERVATION_COORDINATOR"));
        staff.addAll(userService.getUsersByRole("CATERING_TEAM_LEADER"));
        staff.addAll(userService.getUsersByRole("MARKETING_EXECUTIVE"));
        staff.addAll(userService.getUsersByRole("RECEPTIONIST"));
        
        model.addAttribute("staff", staff);
        return "manager/staff";
    }
    
    @GetMapping("/users")
    public String manageAllUsers(Model model) {
        List<User> allUsers = userService.getAllActiveUsers();
        model.addAttribute("users", allUsers);
        return "manager/users";
    }
    
    @GetMapping("/users/create")
    public String createUserForm(Model model) {
        model.addAttribute("user", new User());
        return "manager/create-user";
    }
    
    @PostMapping("/users/create")
    public String createUser(@RequestParam String username,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String firstName,
                           @RequestParam String lastName,
                           @RequestParam String phone,
                           @RequestParam String roleName,
                           RedirectAttributes redirectAttributes) {
        try {
            User user = userService.createUser(username, email, password, firstName, lastName, phone);
            
            // Remove default GUEST role and assign the specified role
            userService.removeRole(user.getUserId(), "GUEST");
            userService.assignRole(user.getUserId(), roleName);
            
            redirectAttributes.addFlashAttribute("success", "User created successfully!");
            return "redirect:/manager/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/manager/users/create";
        }
    }
    
    @PostMapping("/users/{userId}/activate")
    public String activateUser(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        try {
            userService.activateUser(userId);
            redirectAttributes.addFlashAttribute("success", "User activated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/users";
    }
    
    @PostMapping("/users/{userId}/deactivate")
    public String deactivateUser(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        try {
            userService.deactivateUser(userId);
            redirectAttributes.addFlashAttribute("success", "User deactivated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/users";
    }
    
    @PostMapping("/users/{userId}/change-password")
    public String changeUserPassword(@PathVariable Long userId, 
                                   @RequestParam String newPassword,
                                   RedirectAttributes redirectAttributes) {
        try {
            userService.changePassword(userId, newPassword);
            redirectAttributes.addFlashAttribute("success", "Password changed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/users";
    }
    
    @PostMapping("/staff/{userId}/assign-role")
    public String assignRole(@PathVariable Long userId, @RequestParam String roleName, RedirectAttributes redirectAttributes) {
        try {
            userService.assignRole(userId, roleName);
            redirectAttributes.addFlashAttribute("success", "Role assigned successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/staff";
    }
    
    @PostMapping("/staff/{userId}/remove-role")
    public String removeRole(@PathVariable Long userId, @RequestParam String roleName, RedirectAttributes redirectAttributes) {
        try {
            userService.removeRole(userId, roleName);
            redirectAttributes.addFlashAttribute("success", "Role removed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/staff";
    }
    
    @GetMapping("/rooms")
    public String manageRooms(Model model) {
        List<Room> rooms = roomService.getAllRooms();
        model.addAttribute("rooms", rooms);
        return "manager/rooms";
    }
    
    @PostMapping("/rooms/add")
    public String addRoom(@RequestParam String roomName,
                           @RequestParam Room.RoomType roomType,
                           @RequestParam Integer capacity,
                           @RequestParam BigDecimal hourlyRate,
                           @RequestParam(required = false) String description,
                           RedirectAttributes redirectAttributes) {
        try {
            roomService.createRoom(roomName, roomType, capacity, hourlyRate, description);
            redirectAttributes.addFlashAttribute("success", "Room added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/rooms";
    }

    @PostMapping("/rooms/{roomId}/deactivate")
    public String deactivateRoom(@PathVariable Long roomId, RedirectAttributes redirectAttributes) {
        try {
            roomService.deactivateRoom(roomId);
            redirectAttributes.addFlashAttribute("success", "Room deactivated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/rooms";
    }
    
    @PostMapping("/rooms/{roomId}/activate")
    public String activateRoom(@PathVariable Long roomId, RedirectAttributes redirectAttributes) {
        try {
            roomService.activateRoom(roomId);
            redirectAttributes.addFlashAttribute("success", "Room activated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/rooms";
    }
    
    @GetMapping("/notifications")
    public String viewNotifications(@RequestParam(required = false, defaultValue = "ALL") String status,
                                    Authentication authentication, Model model) {
        try {
            User user = getCurrentUser(authentication);
            if (user == null) {
                model.addAttribute("error", "User not found");
                model.addAttribute("notifications", java.util.Collections.emptyList());
                model.addAttribute("unreadCount", 0L);
                model.addAttribute("status", "ALL");
                return "manager/notifications";
            }
            
            System.out.println("Manager notifications - User ID: " + user.getUserId());
            
            List<com.hotel.roomreservation.model.Notification> notifications = new java.util.ArrayList<>();
            Long unreadCount = 0L;
            
            try {
                System.out.println("Manager notifications - Attempting to fetch notifications for user: " + user.getUserId());
                
                switch (status.toUpperCase()) {
                    case "UNREAD":
                        System.out.println("Manager notifications - Fetching UNREAD notifications");
                        notifications = notificationService.getUnreadNotificationsForUser(user.getUserId());
                        break;
                    case "READ":
                        System.out.println("Manager notifications - Fetching READ notifications");
                        notifications = notificationService.getReadNotificationsForUser(user.getUserId());
                        break;
                    default:
                        System.out.println("Manager notifications - Fetching ALL notifications");
                        notifications = notificationService.getNotificationsForUser(user.getUserId());
                }
                
                System.out.println("Manager notifications - Successfully fetched " + notifications.size() + " notifications");
                
                System.out.println("Manager notifications - Attempting to get unread count");
                unreadCount = notificationService.getUnreadNotificationCount(user.getUserId());
                System.out.println("Manager notifications - Unread count: " + unreadCount);
                
            } catch (Exception serviceException) {
                System.err.println("Manager notifications service error: " + serviceException.getMessage());
                serviceException.printStackTrace();
                notifications = new java.util.ArrayList<>();
                unreadCount = 0L;
            }
            
            model.addAttribute("notifications", notifications);
            model.addAttribute("unreadCount", unreadCount);
            model.addAttribute("status", status.toUpperCase());
        } catch (Exception e) {
            System.err.println("Manager notifications error: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("notifications", java.util.Collections.emptyList());
            model.addAttribute("unreadCount", 0L);
            model.addAttribute("status", "ALL");
            model.addAttribute("error", "Unable to load notifications: " + e.getMessage());
        }
        
        // For compose form options (avoid List.of for Java 8 compatibility)
        model.addAttribute("roles", java.util.Arrays.asList("GENERAL_MANAGER","RESERVATION_COORDINATOR","CATERING_TEAM_LEADER","MARKETING_EXECUTIVE","RECEPTIONIST","GUEST"));
        model.addAttribute("alertTypes", java.util.Arrays.asList(
            com.hotel.roomreservation.model.Notification.AlertType.BOOKING_CONFIRMATION,
            com.hotel.roomreservation.model.Notification.AlertType.BOOKING_CANCELLATION,
            com.hotel.roomreservation.model.Notification.AlertType.RESERVATION_REMINDER,
            com.hotel.roomreservation.model.Notification.AlertType.COORDINATION_ALERT,
            com.hotel.roomreservation.model.Notification.AlertType.CATERING_CONFIRMED,
            com.hotel.roomreservation.model.Notification.AlertType.GUEST_ARRIVAL,
            com.hotel.roomreservation.model.Notification.AlertType.PAYMENT_REMINDER,
            com.hotel.roomreservation.model.Notification.AlertType.BOOKING_CHANGE
        ));
        model.addAttribute("showCompose", true);
        return "manager/notifications";
    }

    @PostMapping("/notifications/send")
    public String sendNotification(@RequestParam String targetType,
                                   @RequestParam(required = false) Long userId,
                                   @RequestParam(required = false) String roleName,
                                   @RequestParam String message,
                                   @RequestParam com.hotel.roomreservation.model.Notification.AlertType alertType,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            int count = 0;
            User sender = getCurrentUser(authentication);
            if ("USER".equalsIgnoreCase(targetType)) {
                if (userId == null) throw new RuntimeException("User is required");
                User recipient = userService.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
                notificationService.sendNotification(recipient, sender, message, alertType);
                count = 1;
            } else if ("ROLE".equalsIgnoreCase(targetType)) {
                if (roleName == null || roleName.isBlank()) throw new RuntimeException("Role is required");
                for (User u : userService.getUsersByRole(roleName)) {
                    notificationService.sendNotification(u, sender, message, alertType);
                    count++;
                }
            } else if ("ALL_GUESTS".equalsIgnoreCase(targetType)) {
                for (User u : userService.getUsersByRole("GUEST")) {
                    notificationService.sendNotification(u, sender, message, alertType);
                    count++;
                }
            } else if ("ALL_USERS".equalsIgnoreCase(targetType)) {
                for (User u : userService.getAllActiveUsers()) {
                    notificationService.sendNotification(u, sender, message, alertType);
                    count++;
                }
            } else {
                throw new RuntimeException("Unknown target type");
            }
            redirectAttributes.addFlashAttribute("success", "Notification sent to " + count + " recipient(s).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/notifications";
    }
    
    @PostMapping("/notifications/{notificationId}/mark-read")
    public String markNotificationAsRead(@PathVariable Long notificationId, RedirectAttributes redirectAttributes) {
        try {
            notificationService.markAsRead(notificationId);
            redirectAttributes.addFlashAttribute("success", "Notification marked as read!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/notifications";
    }
    
    @PostMapping("/notifications/{notificationId}/delete")
    public String deleteNotification(@PathVariable Long notificationId, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            if (notificationService.deleteNotificationForUser(notificationId, user.getUserId())) {
                redirectAttributes.addFlashAttribute("success", "Notification deleted successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Notification not found or access denied!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/notifications";
    }
    
    @PostMapping("/notifications/mark-all-read")
    public String markAllNotificationsAsRead(Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            notificationService.markAllAsReadForUser(user.getUserId());
            redirectAttributes.addFlashAttribute("success", "All notifications marked as read!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/notifications";
    }
    
    @PostMapping("/notifications/delete-all")
    public String deleteAllNotifications(Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            notificationService.deleteAllNotificationsForUser(user.getUserId());
            redirectAttributes.addFlashAttribute("success", "All notifications deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/notifications";
    }
    
    @PostMapping("/notifications/bulk-delete")
    public String bulkDeleteNotifications(@RequestParam("notificationIds") List<Long> notificationIds, 
                                       Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            int deletedCount = notificationService.deleteMultipleNotificationsForUser(notificationIds, user.getUserId());
            redirectAttributes.addFlashAttribute("success", deletedCount + " notifications deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/notifications";
    }
    
    
    private User getCurrentUser(Authentication authentication) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                return null;
            }
            return ((com.hotel.roomreservation.service.CustomUserDetailsService.CustomUserPrincipal) 
                    authentication.getPrincipal()).getUser();
        } catch (Exception e) {
            System.err.println("Error getting current user: " + e.getMessage());
            return null;
        }
    }
}
