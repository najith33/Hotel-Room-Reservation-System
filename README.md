# Hotel Room Reservation System

A comprehensive Spring Boot web application for managing hotel room reservations with role-based access control, QR code verification, and advanced reporting features.

## Features

### Core Functionality
- **Room Booking & Preferences**: Reserve available rooms with guest, meal, and special-request preferences
- **Role-Based Dashboard**: Customized interfaces for different user roles
- **Room Availability Tracker**: Real-time room availability management
- **QR/Reference Code Verification**: Secure booking verification system
- **Booking Analytics & Reports**: Comprehensive reporting with multiple export formats
- **Notification & Alert System**: Multi-channel notification system

### User Roles
- **Guest**: Book reservations, view booking history, manage preferences
- **General Manager**: Oversee all operations, manage staff and rooms
- **Reservation Coordinator**: Coordinate room preparation, guest requests, and reservation support
- **Catering Team Leader**: Manage meal and room-service preferences
- **Marketing Executive**: Analyze trends, generate reports, manage promotions
- **Receptionist**: Verify bookings, handle guest arrivals

### Strategy Patterns Implemented
1. **Notification Strategy**: Email, SMS, and In-App notifications
2. **Report Export Strategy**: PDF, CSV, and JSON export formats
3. **Booking Status Strategy**: Pending, Confirmed, Cancelled, and Completed status handling

## Technology Stack

- **Backend**: Spring Boot 3.2.0, Spring Security, Spring Data JPA
- **Database**: MySQL 8.x
- **Frontend**: Thymeleaf, Bootstrap 5, Font Awesome
- **QR Code**: ZXing library
- **Reports**: Apache POI, iText PDF
- **Email**: Spring Boot Mail
- **Build Tool**: Maven

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL Server 8.x and MySQL Workbench
- IDE (IntelliJ IDEA, Eclipse, or VS Code)

## Setup Instructions

### 1. Database Setup

1. Start MySQL Server and open MySQL Workbench
2. Open `docs/database_schema.sql`
3. Run the full script; it creates and selects the `hotel_room_db` database
4. Confirm the schema and seeded demo accounts were created:
   ```sql
   -- Run the SQL script from docs/database_schema.sql
   ```

### 2. Application Configuration

1. Clone or download the project
2. Navigate to the project directory:
   ```bash
   cd HotelRoomReservationSystem
   ```

3. Update database configuration in `src/main/resources/application.yml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/hotel_room_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
       username: root
       password: # Your MySQL password (empty for default XAMPP)
   ```

4. Configure email settings (optional for development):
   ```yaml
   spring:
     mail:
       host: smtp.mailtrap.io
       port: 2525
       username: your-mailtrap-username
       password: your-mailtrap-password
   ```

### 3. Build and Run

1. Build the project:
   ```bash
   mvn clean install
   ```

2. Run the application:
   ```bash
   mvn spring-boot:run
   ```

3. Access the application:
   - URL: http://localhost:8081
   - Default admin credentials:
     - Username: `admin`
     - Password: `password123`

## Default Data

The application comes with sample data:

### Sample Rooms
- Room 101 (Standard, 2 guests, $45/hour)
- Room 102 (Standard, 2 guests, $45/hour)
- Room 201 (Deluxe, 3 guests, $70/hour)
- Room 202 (Deluxe, 3 guests, $75/hour)
- Suite 301 (Suite, 4 guests, $110/hour)
- Suite 302 (Suite, 4 guests, $125/hour)

### Sample Users
- Admin user with General Manager role

## API Endpoints

### Public Endpoints
- `GET /` - Home page
- `GET /login` - Login page
- `GET /register` - Registration page
- `GET /verify-booking` - Booking verification

### Guest Endpoints
- `GET /guest/dashboard` - Guest dashboard
- `GET /guest/book-room` - Room booking form
- `POST /guest/book-room` - Submit booking
- `GET /guest/my-bookings` - View bookings
- `GET /guest/rooms` - View available rooms

### Staff Endpoints
- `GET /manager/dashboard` - Manager dashboard
- `GET /manager/bookings` - View all bookings
- `GET /reception/verify-booking` - Verify bookings
- `GET /marketing/reports` - Generate reports

## Project Structure

```
HotelRoomReservationSystem/
├── src/main/java/com/hotel/roomreservation/
│   ├── config/                 # Configuration classes
│   ├── controller/             # REST controllers
│   ├── model/                  # JPA entities
│   ├── repository/             # Data access layer
│   ├── service/                # Business logic
│   ├── strategy/               # Strategy pattern implementations
│   └── util/                   # Utility classes
├── src/main/resources/
│   ├── templates/              # Thymeleaf templates
│   └── application.yml         # Application configuration
├── docs/
│   └── database_schema.sql     # Database schema
└── pom.xml                     # Maven dependencies
```

## Key Features Explained

### 1. Room Booking System
- Dynamic form with room selection, reservation date/time, and guest count
- Optional room setup preferences and special requests
- Optional meal preferences and dietary requirements
- Automatic cost calculation based on room rates
- QR code generation for booking verification

### 2. Role-Based Access Control
- Spring Security integration
- Role-specific dashboards and permissions
- Secure authentication and authorization

### 3. Notification System
- Strategy pattern for multiple notification channels
- Email notifications (configurable SMTP)
- In-app notifications stored in database
- SMS notifications (mock implementation)

### 4. Reporting System
- Multiple report types (booking analytics, room utilization, revenue)
- Export in PDF, CSV, and JSON formats
- Date range filtering and custom parameters

### 5. QR Code Integration
- Automatic QR code generation for bookings
- Public verification endpoint
- Reception desk verification system

## Development Notes

### Adding New Features
1. Create entity models in `model/` package
2. Add repository interfaces in `repository/` package
3. Implement business logic in `service/` package
4. Create controllers in `controller/` package
5. Add Thymeleaf templates in `resources/templates/`

### Database Changes
1. Update entity models
2. Modify `docs/database_schema.sql`
3. Update repository queries if needed

### Security Configuration
- Modify `SecurityConfig.java` for access control changes
- Update `CustomUserDetailsService.java` for authentication changes

## Troubleshooting

### Common Issues

1. **Database Connection Error**
   - Ensure MySQL Server is running
   - Check database credentials in `application.yml`
   - Verify database `hotel_room_db` exists

2. **Port Already in Use**
   - Change port in `application.yml`:
     ```yaml
     server:
       port: 8081
     ```

3. **Email Not Working**
   - Configure SMTP settings in `application.yml`
   - For development, emails are logged to console

4. **QR Code Not Generating**
   - Check ZXing dependency in `pom.xml`
   - Verify QR code generation service

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is licensed under the MIT License.

## Support

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the documentation in the `docs/` folder
