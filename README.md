# TravelRoulette24
TravelRoulette24 is a Jetpack Compose mobile application that discovers the 20 cheapest travel opportunities departing within the next 24 hours. It supports flights, trains, and ferries, and provides both round-trip and one-way travel modes. The app is designed as a pure travel recommender: users do not book inside the app. Instead, each travel suggestion includes a deep-link to the official provider.

## Key Features

### 1. Real-Time Top 20 Travel Opportunities
- Fetches the cheapest 20 travel options departing within the next 24 hours.
- Supports flights, trains, and ferries.
- Automatically filters by number of passengers.
- Includes online check-in availability.

### 2. Round Trip Mode
- User defines a stay duration window (min/max hours).
- Only returns travel options that satisfy the stay duration constraint.
- Includes return times and full round-trip pricing.

### 3. One-Way Mode with Travel Chaining
- Returns only outbound travel options.
- Once the user arrives at the destination, the app automatically generates a new Top 20 list from the new city.
- Enables spontaneous multi-city travel experiences.

### 4. Opportunity Alerts (Lowest Historical Price)
- Detects when a travel option is at its lowest historical price.
- Uses historical price data from backend services.
- Marks opportunities with a boolean flag `isOpportunity`.

### 5. Travel Budget Guardian (Budget-Friendly Alerts)
- Users can define maximum budgets for:
  - Single trips
  - Round trips
  - Travel chain steps
- The app filters out options that exceed the budget.
- Marks budget-friendly options with `isBudgetFriendly`.
- Enables “Budget Roulette” mode for safe, cost-controlled travel discovery.

### 6. Zero User Input for Transportation Hubs
- Users never type airport names, train stations, or ports.
- The app resolves all transportation hubs automatically from the user’s geolocation.

### 7. Deep-Link Booking
- Each travel option includes a direct link to the official provider.
- The app never handles payments or reservations.
- No user registration required.

## Architecture Overview

### Frontend (Jetpack Compose)
- Modern declarative UI.
- State-driven screens.
- Supports dynamic lists, animations, and travel “roulette” interactions.
- Local storage for budget preferences.

### Backend Services
- Transportation Hub Resolver (city → airports, train stations, ports)
- Real-Time Travel Aggregator (flights, trains, ferries)
- Historical Price Service (12-month and 30-day minimums)
- Travel Intelligence Engine (LLM-based, deterministic JSON output)
- Budget Guardian Filter

### Data Flow
1. App detects user city via geolocation.
2. Backend resolves transportation hubs.
3. Backend fetches real-time travel options.
4. Backend fetches historical price data.
5. Backend applies budget filters.
6. AI engine generates Top 20 list.
7. App displays results and deep-links.

## Permissions
- Location (required): used to determine the origin city.
- Notifications (optional): used for opportunity and budget alerts.

## No Registration Required
TravelRoulette24 does not store personal data and does not require user accounts. All bookings are handled externally via deep-links.

## Tech Stack
- Kotlin
- Jetpack Compose
- Coroutines / Flow
- Retrofit
- Hilt
- Firebase Analytics (optional)

## License
MIT License (or your preferred license)
