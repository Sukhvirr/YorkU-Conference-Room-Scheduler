# YorkU Conference Room Scheduler - Deliverable 2

This project is our Deliverable 2 implementation for the EECS 3311 Conference Room Scheduler. It was developed in Java 17 using Swing and stores application data in CSV files located in the `data` folder.

## Running the Project

Run the application:

```powershell
.\run.bat
```

Compile the project:

```powershell
.\build.bat
```

Run the integration tests:

```powershell
.\test.bat
```

A JDK 17 installation is required. The project also includes a `pom.xml` file for importing into an IDE.

## Demo Accounts

| Role | Email | Password |
|------|-------|----------|
| Registered User | `student@yorku.ca` | `Student123!` |
| Administrator | `admin@yorku.ca` | `Admin123!` |
| Chief Event Coordinator | `chief@yorku.ca` | `Chief123!` |

Delete the CSV files inside the `data` folder if you want to reset the demo data.

## Design Patterns Used

- Singleton - `DatabaseManager`
- Factory Method - `AccountFactory`
- Facade - `SchedulerFacade`
- Strategy - `PaymentStrategy`
- Observer - `BookingObserver`
- State - `BookingState`

## Features

- User registration and login for students, faculty, staff, and partners
- Correct hourly pricing and one-hour upfront deposits
- Administrator account management
- Conference room search and booking
- Credit card, debit card, and institutional billing payments
- Badge check-in and room sensors
- Room management (add, update, enable/disable, maintenance)
- Booking editing, cancellation, and extensions
- Double-booking prevention
- Notifications for booking events
- Data saved automatically using CSV files

## Project Structure

- `App.java` - application entry point
- `MainFrame.java` - Swing user interface
- `SchedulerFacade.java` - main application logic
- `DatabaseManager.java` - CSV file management
- `IntegrationTest.java` - integration tests
