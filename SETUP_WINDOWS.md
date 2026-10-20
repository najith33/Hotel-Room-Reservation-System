# Windows Local Setup

## 1. Database

This project uses **MySQL** and the database name is `hotel_room_db`.

If you previously imported the older schema and saw foreign-key messages such as
`Referencing column ... are incompatible`, recreate the local development database so the corrected BIGINT schema is used:

```sql
DROP DATABASE IF EXISTS hotel_room_db;
```

Then open `docs/database_schema.sql` in MySQL Workbench and run the complete script.

The default configuration in `src/main/resources/application.yml` is:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/hotel_room_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password:
```

If your local MySQL `root` account has a password, set it in `application.yml` before starting the app.

## 2. Run

From the folder that contains `pom.xml`:

```powershell
mvn spring-boot:run
```

Open:

```text
http://localhost:8081/login
```

## 3. Demo accounts

The seeded users are documented in `USER_ACCOUNTS.md`. The project documentation states the demo password is `password123`.
