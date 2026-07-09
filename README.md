# mini.gg

A League of Legends analytics portfolio project inspired by OP.GG and U.GG.

Users can search a Riot ID, such as `GameName#TagLine`, and view Summoner's Rift and TFT profile data, ranked stats, recent match history, and simple performance analytics.

## Tech Stack

- Frontend: React with Vite
- Backend: Spring Boot
- Database: PostgreSQL
- Cache: PostgreSQL freshness cache for now; Redis can be added later
- External API: Riot Games API

## Project Structure

```text
mini.gg/
  backend/    Spring Boot REST API
  frontend/   React dashboard
```

## Features

- Search by Riot ID in `GameName#TagLine` format.
- Fetch Riot account data and PUUID from Account-V1.
- Fetch summoner level from Summoner-V4.
- Fetch ranked solo queue data from League-V4.
- Fetch the latest 10 matches from Match-V5.
- Calculate recent win rate, average KDA, and most played champion.
- Fetch TFT ranked data from TFT League-V1.
- Fetch recent TFT matches from TFT Match-V1.
- Calculate TFT average placement, top 4 rate, first place rate, and most played unit.
- Store searched players and recent matches in PostgreSQL.
- Reuse fresh database data before calling Riot again.
- Switch between Summoner's Rift and TFT tabs after one shared Riot ID search.
- Show loading and error states in the React UI.

## Backend Setup

Requirements:

- Java 17
- Maven
- PostgreSQL
- Riot Games development API key

Create a local database:

```bash
createdb mini_opgg
```

Or with Docker:

```bash
docker run --name mini-gg-postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=mini_opgg -p 5432:5432 -d postgres:16
```

Create `backend/.env` from `backend/.env.example`:

```env
RIOT_API_KEY=RGAPI-your-development-key
RIOT_REGIONAL_ROUTE=americas
RIOT_PLATFORM_ROUTE=na1
RIOT_CACHE_TTL_MINUTES=30
DATABASE_URL=jdbc:postgresql://localhost:5432/mini_opgg
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres
```

Run the backend from the `backend` folder:

```bash
mvn spring-boot:run
```

Spring Boot does not automatically read `.env` files by itself. In a terminal, export the values first, use your IDE run configuration, or pass them inline:

```bash
RIOT_API_KEY=RGAPI-your-development-key mvn spring-boot:run
```

On Windows PowerShell:

```powershell
$env:RIOT_API_KEY="RGAPI-your-development-key"
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/mini_opgg"
$env:DATABASE_USERNAME="postgres"
$env:DATABASE_PASSWORD="postgres"
mvn spring-boot:run
```

The API runs at `http://localhost:8080`.

## Frontend Setup

Requirements:

- Node.js
- npm

Create `frontend/.env` from `frontend/.env.example`:

```env
VITE_API_BASE_URL=http://localhost:8080
```

Run the frontend from the `frontend` folder:

```bash
npm install
npm run dev
```

On Windows PowerShell, if `npm` is blocked by script execution policy, use:

```bash
npm.cmd install
npm.cmd run dev
```

The React app normally runs at `http://localhost:5173`.

## API Endpoints

```http
GET /api/player/{gameName}/{tagLine}
GET /api/player/{gameName}/{tagLine}/rank
GET /api/player/{gameName}/{tagLine}/matches
GET /api/player/{gameName}/{tagLine}/summary

GET /api/tft/player/{gameName}/{tagLine}
GET /api/tft/player/{gameName}/{tagLine}/rank
GET /api/tft/player/{gameName}/{tagLine}/matches
GET /api/tft/player/{gameName}/{tagLine}/summary
```

## Riot Region Notes

Riot APIs use two route types:

- `RIOT_REGIONAL_ROUTE`: used by Account-V1, Match-V5, and TFT Match-V1, for example `americas`, `asia`, `europe`.
- `RIOT_PLATFORM_ROUTE`: used by Summoner-V4, League-V4, TFT Summoner-V1, and TFT League-V1, for example `na1`, `euw1`, `kr`.

For a North America portfolio demo, use:

```env
RIOT_REGIONAL_ROUTE=americas
RIOT_PLATFORM_ROUTE=na1
```

## Database Tables

Spring Boot creates tables automatically with `spring.jpa.hibernate.ddl-auto=update`:

- `players`: Riot ID, PUUID, summoner level, ranked stats, cache timestamps.
- `matches`: recent match rows linked to a player.
- `tft_players`: Riot ID, PUUID, TFT summoner level, TFT ranked stats, cache timestamps.
- `tft_matches`: recent TFT match rows linked to a TFT player.

This is convenient for development. For production, replace it with Flyway or Liquibase migrations.

## Screenshots

Add screenshots here after running the app locally:

- Search dashboard
- Ranked card
- Recent match history
- Mobile layout

## Portfolio Description

mini.gg is a full-stack League of Legends and TFT analytics dashboard that demonstrates external API integration, backend service layering, DTO-based API responses, PostgreSQL persistence, cache freshness checks, and a responsive React UI.

## What Was Built By Stage

### Stage 1: Project Setup

- Created separate `frontend` and `backend` folders.
- Added a Vite React app.
- Added a Spring Boot app skeleton.
- Added setup documentation and `.gitignore`.

### Stage 2: Backend Riot API Setup

- Added `RIOT_API_KEY`, regional route, platform route, and cache TTL configuration.
- Added `RiotApiProperties`.
- Added `RiotApiService`.
- API keys are read from environment variables and are not hardcoded.

### Stage 3: Player Profile Endpoint

- Added `GET /api/player/{gameName}/{tagLine}`.
- Returns `gameName`, `tagLine`, `puuid`, and `summonerLevel`.
- Added global error handling for player-not-found and Riot API errors.

### Stage 4: Ranked Info

- Added ranked solo queue lookup through League-V4.
- Added `GET /api/player/{gameName}/{tagLine}/rank`.
- Returns tier, rank, LP, wins, losses, and win rate.

### Stage 5: Match History

- Added recent 10 match lookup through Match-V5.
- Added `GET /api/player/{gameName}/{tagLine}/matches`.
- Returns champion, KDA, win/loss, CS, duration, mode, and played date.

### Stage 6: Performance Summary

- Added `GET /api/player/{gameName}/{tagLine}/summary`.
- Calculates analyzed games, recent win rate, average K/D/A, average KDA, and most played champion.

### Stage 7: PostgreSQL Database

- Added Spring Data JPA and PostgreSQL support.
- Added `Player` and `PlayerMatch` entities.
- Added `PlayerRepository`.
- Added freshness checks before calling Riot API again.

### Stage 8: React Frontend

- Added Riot ID search form.
- Added API client.
- Displays profile, ranked info, match history, and summary.
- Added loading and error states.

### Stage 9: UI Polish

- Styled the app as a modern gaming analytics dashboard.
- Added cards, badges, match rows, win/loss colors, and responsive layout.

### Stage 10: README and Final Cleanup

- Added full setup instructions.
- Added environment variable examples.
- Added API endpoint documentation.
- Added portfolio description and screenshots section.

### Stage 11: TFT Support

- Added separate TFT backend endpoints under `/api/tft/player`.
- Added `TftController`, `TftApiService`, and `TftAnalyticsService`.
- Added TFT DTOs for profile, rank, matches, and summary.
- Added `TftPlayer` and `TftMatch` entities plus `TftPlayerRepository`.
- Added PostgreSQL-backed TFT match history caching.
- Added frontend tabs for Summoner's Rift and TFT.
- TFT tab displays rank, LP, wins/losses, recent placements, average placement, top 4 rate, and first place rate.
