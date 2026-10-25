# Booking form validation patch

The booking create/edit forms can submit without optional Décor/Catering preferences.

## Fixed
- Empty mealType / servingStyle select values are not parsed into Java enums.
- Blank optional décor fields do not create empty décor records.
- Catering preferences are only created if a meal type is selected. If other catering details are supplied without a meal type, a friendly error is shown (the database requires a meal type for a catering row).
- Required booking fields are checked on the server, so missing fields redirect back with a readable message instead of a 500 error.
- The form validation scripts have been moved inside the Thymeleaf `content` fragment so they are rendered. Browser native required-field checks are enabled.
- SecurityConfig's accidental rename of `maxSessionsPreventsLogin` is repaired in this ZIP.

## Important
- This ZIP is based on the Hotel Room Reservation System previously supplied in this chat. It does not include your PC's MySQL password. Retain your current `application.yml` datasource credentials when upgrading.
- Don't drop the existing database: these are source-level validation fixes.
- The original system requires a future reservation date (not today) and a booking duration of at least one hour.
- Maven tests were not executed in this environment, so verify locally with `mvn clean test` and `mvn spring-boot:run`.
