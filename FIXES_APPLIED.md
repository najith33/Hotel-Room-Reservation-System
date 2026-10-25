# Fixes Applied

- Restored missing Thymeleaf templates from the project's existing compiled `target/classes/templates` resources into `src/main/resources/templates` (including `login.html`, `register.html`, dashboards, booking pages, profile, and public pages).
- Added the missing `availability/search-results.html` template referenced by `AvailabilityController`.
- Fixed `JSONExportStrategy` empty-list output so the included JUnit test expecting `[]` passes.
- Updated the MySQL Connector dependency to `com.mysql:mysql-connector-j`.
- Updated Hibernate dialect configuration to `MySQLDialect` and corrected the local QR base URL to port `8081`.
- Aligned database ID columns with Java `Long` mappings by changing schema ID/FK columns from `INT` to `BIGINT`.
- Added the missing booking coordination/catering columns and promotions table to the SQL schema.
- Added `/about` and `/contact` to public Spring Security routes.
- Fixed guest access ordering for `/availability/details/**`.
- Added the missing `POST /manager/rooms/add` handler, added hourly-rate input, removed unsupported `OUTDOOR` room type, and made the manager room page show active and inactive rooms.
- Added `SETUP_WINDOWS.md` with clean database/startup steps.


## Room Reservation Conversion

- Rebranded the application to Hotel Room Reservation System.
- Renamed the Java package and main application class.
- Converted Venue/Availability domain naming to Room/RoomAvailability.
- Converted event-specific booking terminology to stay/reservation terminology.
- Updated room types to Standard, Deluxe, and Suite.
- Updated routes, templates, security roles, reports, notifications, database schema, and documentation.
