# Hotel Room Reservation Conversion Notes

This version converts the original Hotel Event Reservation System into a **Hotel Room Reservation System** while preserving the original Spring Boot architecture and role-based workflow.

## Main changes

- Project/application name changed to `HotelRoomReservationSystem`.
- Java base package changed from `com.hotel.eventreservation` to `com.hotel.roomreservation`.
- Maven artifact changed to `room-reservation-system`.
- Database changed from `hotel_event_db` to `hotel_room_db`.
- `Venue` domain classes were converted to `Room` (`Room`, `RoomService`, `RoomRepository`, `RoomAvailability`, and related code).
- Room categories are now `STANDARD`, `DELUXE`, and `SUITE`.
- Guest routes changed from `/guest/book-event` and `/guest/venues` to `/guest/book-room` and `/guest/rooms`.
- Manager room management is available under `/manager/rooms`.
- Booking fields were renamed from event terminology to room-stay terminology (`stayType`, `reservationDate`).
- `EVENT_COORDINATOR` was changed to `RESERVATION_COORDINATOR`.
- `EVENT_REMINDER` was changed to `RESERVATION_REMINDER`.
- UI headings, navigation, reports, notifications, PDF/email branding, documentation, and database schema were updated to room-reservation terminology.
- Existing optional décor/setup and catering/meal preferences were retained as room-stay add-on preferences.

## Important database step

This converted project uses a different schema and role names. Recreate the local database before running it:

```sql
DROP DATABASE IF EXISTS hotel_room_db;
```

Then execute the complete `docs/database_schema.sql` file in MySQL Workbench.

## Run

Update the MySQL password in `src/main/resources/application.yml` if required, then run:

```powershell
mvn clean
mvn spring-boot:run
```

Open:

```text
http://localhost:8081/login
```

The seeded demo password documented by the project is `password123`.
