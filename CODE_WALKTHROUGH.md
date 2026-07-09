# mini.gg Code Walkthrough

This guide explains how the mini.gg code works from top to bottom. It is written for someone learning full-stack development, so it focuses on the "why" behind each file and how data moves through the app.

## 1. What This Project Is

mini.gg is a League of Legends analytics dashboard.

A user enters a Riot ID like:

```text
GameName#TagLine
```

The app then shows:

- Basic player profile data
- Ranked solo queue data, when Riot allows access
- Recent match history
- A simple performance summary

The project has three main parts:

```text
frontend/   React app the user sees in the browser
backend/    Spring Boot API that talks to Riot and PostgreSQL
database    PostgreSQL tables for cached player and match data
```

## 2. The Big Picture Flow

Here is what happens when someone searches a player:

```text
User types Riot ID
  -> React validates GameName#TagLine
  -> React calls the Spring Boot backend
  -> Backend checks PostgreSQL cache
  -> Backend calls Riot API if data is missing or stale
  -> Backend saves fresh data to PostgreSQL
  -> Backend returns clean DTO responses
  -> React renders cards, stats, and match rows
```

The important idea is that the frontend does not call Riot directly. It calls your backend. That matters because the Riot API key should stay private.

## 3. Frontend Entry Point

File:

```text
frontend/src/main.jsx
```

This file starts the React app.

```jsx
createRoot(document.getElementById('root')).render(...)
```

The browser loads `frontend/index.html`, which contains:

```html
<div id="root"></div>
```

React finds that `root` div and renders the `App` component inside it.

## 4. The Main React Component

File:

```text
frontend/src/App.jsx
```

This is the main UI file. It contains:

- The search form
- Loading state
- Error state
- Profile card
- Ranked card
- Summary cards
- Recent match list

### State Variables

At the top of `App`, these state values are created:

```jsx
const [riotId, setRiotId] = useState('');
const [dashboard, setDashboard] = useState(null);
const [status, setStatus] = useState('idle');
const [error, setError] = useState('');
```

They mean:

- `riotId`: what the user typed into the search box
- `dashboard`: the data returned from the backend
- `status`: whether the UI is idle, loading, successful, or errored
- `error`: the message shown if something goes wrong

### Calculating Recent Record

```jsx
const record = useMemo(() => {
  const wins = matches.filter((match) => match.win).length;
  return { wins, losses: matches.length - wins };
}, [matches]);
```

This counts wins and losses from recent matches.

`useMemo` means "only recalculate this when `matches` changes." It is not required for a tiny app, but it is a common React pattern for derived values.

### Search Submit

The search form calls:

```jsx
handleSubmit(event)
```

That function:

1. Stops the browser from refreshing the page.
2. Clears old errors.
3. Parses the Riot ID.
4. Shows the loading UI.
5. Calls the backend through `fetchPlayerDashboard`.
6. Saves returned data into React state.
7. Shows an error if the request fails.

This line validates the Riot ID:

```jsx
const parsed = parseRiotId(riotId);
```

If the user does not type exactly one `#`, the app shows:

```text
Enter a Riot ID in the format GameName#TagLine.
```

### Rendering the Dashboard

This block only renders after data exists:

```jsx
{dashboard && (
  <section className="dashboard-grid">
    ...
  </section>
)}
```

That is a common React pattern. If `dashboard` is `null`, nothing shows. If it has data, the dashboard appears.

## 5. Frontend API Client

File:

```text
frontend/src/api.js
```

This file is responsible for HTTP requests from React to Spring Boot.

```js
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';
```

That means:

- If `VITE_API_BASE_URL` exists, use it.
- Otherwise use `http://localhost:8080`.

### Timeout Handling

```js
const REQUEST_TIMEOUT_MS = 25000;
```

The frontend cancels a request after 25 seconds. This prevents the button from saying "Searching" forever.

The timeout uses:

```js
AbortController
```

That is a browser feature that can cancel a `fetch` request.

### Request Order

```js
const profile = await getJson(basePath);
const [rank, matches] = await Promise.all([
  getJson(`${basePath}/rank`),
  getJson(`${basePath}/matches`),
]);
const summary = await getJson(`${basePath}/summary`);
```

The frontend first gets the player profile. Then it requests rank and matches at the same time. Then it gets the summary.

The reason summary happens last is that the summary depends on match data. Doing it last avoids asking Riot for the same match list twice during the first search.

## 6. Frontend Styling

File:

```text
frontend/src/styles.css
```

This file controls the look of the app:

- Dark dashboard background
- Search panel layout
- Cards
- Match rows
- Win/loss colors
- Mobile responsiveness

Important class names:

- `.app-shell`: whole page container
- `.search-panel`: top search area
- `.dashboard-grid`: main card layout
- `.panel`: reusable card style
- `.match-row.win`: blue win row
- `.match-row.loss`: pink/red loss row

The frontend is intentionally simple: it uses plain CSS instead of a component library.

## 7. Backend Entry Point

File:

```text
backend/src/main/java/com/miniopgg/MiniOpggApplication.java
```

This starts the Spring Boot app.

```java
SpringApplication.run(MiniOpggApplication.class, args);
```

It also enables Riot config properties:

```java
@EnableConfigurationProperties(RiotApiProperties.class)
```

That tells Spring to load values like `RIOT_API_KEY`, `RIOT_REGIONAL_ROUTE`, and `RIOT_PLATFORM_ROUTE`.

## 8. Backend Configuration

File:

```text
backend/src/main/resources/application.yml
```

This file defines backend settings.

### App Name

```yaml
spring:
  application:
    name: mini-gg-backend
```

This is the Spring Boot application name.

### Database Connection

```yaml
datasource:
  url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/mini_opgg}
  username: ${DATABASE_USERNAME:postgres}
  password: ${DATABASE_PASSWORD:postgres}
```

This means:

- Use `DATABASE_URL` if it exists.
- Otherwise default to local PostgreSQL database `mini_opgg`.

The syntax:

```text
${ENV_VAR:default_value}
```

means "read this environment variable, or use the default."

### JPA

```yaml
jpa:
  hibernate:
    ddl-auto: update
```

This lets Hibernate create/update database tables based on your Java entity classes.

This is convenient for learning and local development. In a real production app, teams usually use migration tools like Flyway or Liquibase.

### Riot Config

```yaml
riot:
  api-key: ${RIOT_API_KEY:}
  regional-route: ${RIOT_REGIONAL_ROUTE:americas}
  platform-route: ${RIOT_PLATFORM_ROUTE:na1}
  cache-ttl-minutes: ${RIOT_CACHE_TTL_MINUTES:30}
```

For NA:

```text
regional-route = americas
platform-route = na1
```

Riot uses different route types:

- Account and Match APIs use regional routes like `americas`.
- Summoner and League APIs use platform routes like `na1`.

## 9. Riot Properties Class

File:

```text
backend/src/main/java/com/miniopgg/config/RiotApiProperties.java
```

This record stores Riot config:

```java
public record RiotApiProperties(
    String apiKey,
    String regionalRoute,
    String platformRoute,
    long cacheTtlMinutes
) {}
```

Spring fills this object from `application.yml`.

Instead of reading environment variables manually in every service, the app injects this one object wherever Riot settings are needed.

## 10. HTTP Client Config

File:

```text
backend/src/main/java/com/miniopgg/config/AppConfig.java
```

This creates the backend HTTP client used to call Riot:

```java
RestClient.Builder restClientBuilder()
```

It also sets timeouts:

```java
requestFactory.setConnectTimeout(Duration.ofSeconds(5));
requestFactory.setReadTimeout(Duration.ofSeconds(12));
```

That means:

- Give up if Riot cannot be connected to within 5 seconds.
- Give up if Riot takes more than 12 seconds to respond.

This prevents backend requests from hanging forever.

## 11. CORS Config

File:

```text
backend/src/main/java/com/miniopgg/config/CorsConfig.java
```

Browsers block frontend-to-backend requests if the frontend and backend are on different ports unless CORS allows it.

Your frontend runs on:

```text
http://localhost:5173
```

Your backend runs on:

```text
http://localhost:8080
```

Because the ports differ, the browser treats them as different origins.

This config allows the React frontend to call `/api/**` endpoints.

## 12. Controller Layer

File:

```text
backend/src/main/java/com/miniopgg/controller/PlayerController.java
```

Controllers handle HTTP requests and responses.

This controller starts with:

```java
@RestController
@RequestMapping("/api/player")
```

That means every route starts with:

```text
/api/player
```

### Endpoints

Profile:

```java
GET /api/player/{gameName}/{tagLine}
```

Rank:

```java
GET /api/player/{gameName}/{tagLine}/rank
```

Matches:

```java
GET /api/player/{gameName}/{tagLine}/matches
```

Summary:

```java
GET /api/player/{gameName}/{tagLine}/summary
```

The controller does not contain business logic. It delegates to:

```java
PlayerAnalyticsService
```

That is good architecture. Controllers should stay thin.

## 13. DTOs

Folder:

```text
backend/src/main/java/com/miniopgg/dto
```

DTO means Data Transfer Object.

DTOs are the shapes of data returned by the API.

Examples:

- `PlayerProfileResponse`
- `RankedInfoResponse`
- `MatchSummaryResponse`
- `PerformanceSummaryResponse`
- `ErrorResponse`

Why use DTOs?

Because you do not want to return raw database entities or raw Riot API JSON to the frontend. DTOs let you control exactly what the browser receives.

Example:

```java
public record PlayerProfileResponse(
    String gameName,
    String tagLine,
    String puuid,
    Long summonerLevel
) {}
```

This is the profile response shape.

Records are useful here because response objects are simple data containers.

## 14. Entity Layer

Folder:

```text
backend/src/main/java/com/miniopgg/entity
```

Entities are Java classes that map to database tables.

### Player Entity

File:

```text
Player.java
```

This maps to the `players` table:

```java
@Entity
@Table(name = "players")
```

Important fields:

- `gameName`
- `tagLine`
- `puuid`
- `summonerId`
- `summonerLevel`
- `tier`
- `rank`
- `leaguePoints`
- `wins`
- `losses`
- `profileUpdatedAt`
- `rankUpdatedAt`
- `matchesUpdatedAt`

The timestamp fields are used for caching.

For example:

```java
profileUpdatedAt
```

tells the app when profile data was last fetched.

### PlayerMatch Entity

File:

```text
PlayerMatch.java
```

This maps to the `matches` table.

Important fields:

- `matchId`
- `championName`
- `kills`
- `deaths`
- `assists`
- `win`
- `cs`
- `gameDuration`
- `gameMode`
- `playedAt`

### Relationship Between Player and Matches

In `Player.java`:

```java
@OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
private List<PlayerMatch> matches = new ArrayList<>();
```

This means:

- One player can have many match rows.
- If a player's match list is replaced, old match rows can be removed.
- Saving the player can save the related matches too.

In `PlayerMatch.java`:

```java
@ManyToOne(fetch = FetchType.LAZY)
private Player player;
```

This means each match belongs to one player.

## 15. Repository Layer

File:

```text
backend/src/main/java/com/miniopgg/repository/PlayerRepository.java
```

Repositories are how Spring Data JPA talks to the database.

```java
public interface PlayerRepository extends JpaRepository<Player, Long>
```

This gives the app methods like:

- `save`
- `findById`
- `findAll`
- `delete`

Custom methods:

```java
findByGameNameIgnoreCaseAndTagLineIgnoreCase(...)
```

This finds a player by Riot ID.

```java
findByPuuid(...)
```

This finds a player by PUUID and also loads matches because of:

```java
@EntityGraph(attributePaths = "matches")
```

That avoids lazy loading problems when the service needs matches.

## 16. Riot API Service

File:

```text
backend/src/main/java/com/miniopgg/service/RiotApiService.java
```

This service only focuses on Riot API communication.

That is its job:

- Build Riot URLs
- Add the API key header
- Parse Riot responses
- Convert Riot JSON into small Java records
- Throw app-specific errors when Riot fails

### API Key Handling

Every Riot request goes through:

```java
getJson(...)
```

Before the request, it calls:

```java
ensureApiKeyConfigured();
```

If no key is configured, it throws:

```text
RIOT_API_KEY is not configured.
```

The key is sent with:

```java
.header("X-Riot-Token", properties.apiKey())
```

That is the header Riot expects.

### Searching a Player

```java
searchPlayerByRiotId(String gameName, String tagLine)
```

Calls:

```text
/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}
```

This returns:

- gameName
- tagLine
- puuid

PUUID is Riot's stable player identifier.

### Getting Summoner Info

```java
getSummonerByPuuid(String puuid)
```

Calls:

```text
/lol/summoner/v4/summoners/by-puuid/{puuid}
```

This returns:

- summoner level

The summoner id is stored for reference, but rank lookup now uses PUUID.

### Getting Ranked Info

```java
getSoloQueueRank(String puuid)
```

Calls:

```text
/lol/league/v4/entries/by-puuid/{puuid}
```

Riot can return multiple queues. The code looks for:

```text
RANKED_SOLO_5x5
```

If found, it returns:

- tier
- rank
- leaguePoints
- wins
- losses

If no solo queue entry exists, it returns `null`, which means unranked.

### Getting Match IDs

```java
getRecentMatchIds(String puuid, int count)
```

Calls:

```text
/lol/match/v5/matches/by-puuid/{puuid}/ids?start=0&count=10
```

This returns a list of match IDs.

### Getting Match Details

```java
getMatchDetails(String matchId, String puuid)
```

Calls:

```text
/lol/match/v5/matches/{matchId}
```

Then it finds the searched player inside the match participants:

```java
findParticipant(info.path("participants"), puuid)
```

It returns only that player's performance in the match.

CS is calculated as:

```java
totalMinionsKilled + neutralMinionsKilled
```

### Error Handling

Common Riot errors are converted into `RiotApiException`:

- `404`: resource not found
- `429`: rate limit
- other 4xx/5xx: Riot request failed
- timeout: Riot request timed out

This keeps Riot-specific errors from leaking all over the codebase.

## 17. Player Analytics Service

File:

```text
backend/src/main/java/com/miniopgg/service/PlayerAnalyticsService.java
```

This is the main business logic service.

It decides:

- Should we use cached database data?
- Should we call Riot?
- How do we calculate win rate and KDA?
- How do we transform entities into DTOs?

### Profile Flow

```java
getProfile(gameName, tagLine)
```

Calls:

```java
getOrRefreshPlayer(gameName, tagLine)
```

That method:

1. Checks PostgreSQL for an existing player.
2. Checks if the data is fresh.
3. Uses cached data if fresh.
4. Calls Riot if missing or stale.
5. Saves the player.

Freshness is checked by:

```java
isFresh(player.getProfileUpdatedAt())
```

The cache TTL comes from:

```text
RIOT_CACHE_TTL_MINUTES
```

Default is 30 minutes.

### Rank Flow

```java
getRank(gameName, tagLine)
```

It fetches or refreshes rank data.

If Riot returns `403` for the ranked endpoint, the app stores:

```text
UNAVAILABLE
```

That means:

- The app still works.
- Profile and matches can still load.
- If ranked access works later, the app retries instead of treating unavailable as permanent.

If Riot returns no solo queue entry, the player is:

```text
UNRANKED
```

That is different from `UNAVAILABLE`.

### Match Flow

```java
getMatches(gameName, tagLine)
```

It:

1. Gets or refreshes the player.
2. Loads the player's cached matches.
3. If matches are stale or fewer than 10, calls Riot.
4. Saves fresh match rows.
5. Sorts matches newest first.
6. De-duplicates matches by `matchId`.
7. Converts match entities to DTOs.

The de-duplication protects the UI from showing the same match twice if old duplicate rows are already in the database.

### Summary Flow

```java
getSummary(gameName, tagLine)
```

It uses the recent matches to calculate:

- total games analyzed
- recent win rate
- average kills
- average deaths
- average assists
- average KDA
- most played champion

Average KDA is calculated as:

```java
(kills + assists) / Math.max(1, deaths)
```

`Math.max(1, deaths)` avoids dividing by zero.

## 18. Exception Handling

File:

```text
backend/src/main/java/com/miniopgg/controller/GlobalExceptionHandler.java
```

This catches exceptions and turns them into clean JSON responses.

Example error response:

```json
{
  "timestamp": "2026-06-29T...",
  "status": 404,
  "error": "Not Found",
  "message": "No Riot account found for GameName#TagLine."
}
```

Without this file, Spring Boot would return less friendly default error pages or stack traces.

The frontend reads `message` and displays it to the user.

## 19. Why The API Key Is Not Hardcoded

The Riot API key is private. It should not be committed to GitHub.

That is why the backend reads:

```text
RIOT_API_KEY
```

from the environment.

In PowerShell:

```powershell
$env:RIOT_API_KEY="RGAPI-your-key"
```

Then:

```powershell
mvn.cmd spring-boot:run
```

The key must be set in the same terminal window that starts the backend.

## 20. Why PostgreSQL Is Used

Riot APIs have rate limits and can be slow.

PostgreSQL helps by storing:

- players already searched
- recent match data
- rank data
- timestamps showing when data was last refreshed

Instead of calling Riot every time, the app can reuse fresh data.

That is the basic idea of caching.

## 21. Why Redis Was Skipped

Redis is a fast in-memory cache, but it adds another service to install and run.

For this learning project, PostgreSQL caching is enough.

A later version could add Redis for:

- short-lived player search cache
- rate limit protection
- faster repeated match lookups

## 22. Important Backend Design Pattern

The backend follows this structure:

```text
Controller -> Service -> Repository / External API
```

In this project:

```text
PlayerController
  -> PlayerAnalyticsService
    -> PlayerRepository
    -> RiotApiService
```

Why this is good:

- Controllers stay simple.
- Business logic lives in services.
- Database access lives in repositories.
- Riot API logic is isolated in one service.
- DTOs define clean API responses.

This is the kind of structure interviewers like to see in portfolio projects.

## 23. Important Frontend Design Pattern

The frontend follows this structure:

```text
App.jsx
  -> UI state
  -> form handling
  -> presentational components
api.js
  -> backend HTTP requests
styles.css
  -> visual styling
```

The frontend does not know how Riot works. It only knows your backend endpoints.

That separation is important.

## 24. Full Search Example

Suppose the user searches:

```text
Minju#illit
```

The frontend splits it into:

```text
gameName = Minju
tagLine = illit
```

Then it calls:

```text
GET http://localhost:8080/api/player/Minju/illit
```

Backend:

1. Checks `players` table.
2. If missing or stale, calls Riot Account-V1.
3. Gets PUUID.
4. Calls Riot Summoner-V4.
5. Saves player.
6. Returns profile DTO.

Frontend then calls:

```text
GET /api/player/Minju/illit/rank
GET /api/player/Minju/illit/matches
GET /api/player/Minju/illit/summary
```

Then the UI renders the dashboard.

## 25. How To Add A New Stat

Example: average CS.

Backend:

1. Add a field to `PerformanceSummaryResponse`.
2. Calculate average CS in `PlayerAnalyticsService.getSummary`.
3. Return it in the response.

Frontend:

1. Add another `<Stat />` in `SummaryCards`.
2. Style is already handled by `.summary-strip`.

This is a good way to extend the project.

## 26. How To Change The Number Of Matches

File:

```text
PlayerAnalyticsService.java
```

Change:

```java
private static final int RECENT_MATCH_COUNT = 10;
```

For example:

```java
private static final int RECENT_MATCH_COUNT = 20;
```

Be careful: more matches means more Riot API calls, so searches can get slower.

## 27. How To Change Regions

For NA:

```powershell
$env:RIOT_REGIONAL_ROUTE="americas"
$env:RIOT_PLATFORM_ROUTE="na1"
```

For EUW:

```powershell
$env:RIOT_REGIONAL_ROUTE="europe"
$env:RIOT_PLATFORM_ROUTE="euw1"
```

For Korea:

```powershell
$env:RIOT_REGIONAL_ROUTE="asia"
$env:RIOT_PLATFORM_ROUTE="kr"
```

Then restart the backend.

## 28. Common Problems

### 403 Forbidden

Usually means Riot rejected the key for that endpoint.

If Account and Summoner work but League-V4 fails, ranked data may show as:

```text
UNAVAILABLE
```

The rest of the app can still work.

### Backend Cannot Connect To Database

Check PostgreSQL is running and the database exists:

```text
mini_opgg
```

### Search Spins Too Long

The app now has timeouts, but if this happens:

1. Check backend terminal logs.
2. Check Riot API key.
3. Check routes.
4. Restart backend and frontend.

### Frontend Cannot Reach Backend

Make sure backend is running on:

```text
http://localhost:8080
```

and frontend is running on:

```text
http://localhost:5173
```

## 29. What To Study First

If you are learning from this project, study in this order:

1. `frontend/src/App.jsx`
2. `frontend/src/api.js`
3. `backend/src/main/java/com/miniopgg/controller/PlayerController.java`
4. `backend/src/main/java/com/miniopgg/service/PlayerAnalyticsService.java`
5. `backend/src/main/java/com/miniopgg/service/RiotApiService.java`
6. `backend/src/main/java/com/miniopgg/entity/Player.java`
7. `backend/src/main/java/com/miniopgg/entity/PlayerMatch.java`
8. `backend/src/main/resources/application.yml`

That order follows the same path a real user request takes.

## 30. The Most Important Concepts

If you understand these, you understand the project:

- React state controls what appears on screen.
- `api.js` calls the backend.
- Controllers expose HTTP routes.
- Services contain business logic.
- Repositories talk to PostgreSQL.
- Entities become database tables.
- DTOs define API response shapes.
- Riot API calls happen only in `RiotApiService`.
- Environment variables keep secrets out of code.
- Cache timestamps prevent unnecessary Riot calls.
- Error handlers turn exceptions into clean frontend messages.

That is the backbone of the app.

## 31. TFT Support Add-On

TFT support was added as a separate feature path so the existing Summoner's Rift endpoints keep working.

The new backend endpoints are:

```text
GET /api/tft/player/{gameName}/{tagLine}
GET /api/tft/player/{gameName}/{tagLine}/rank
GET /api/tft/player/{gameName}/{tagLine}/matches
GET /api/tft/player/{gameName}/{tagLine}/summary
```

The frontend still uses one Riot ID input:

```text
GameName#TagLine
```

After a search, the UI shows two tabs:

```text
Summoner's Rift
TFT
```

Summoner's Rift keeps using the original backend endpoints under:

```text
/api/player
```

TFT uses the new backend endpoints under:

```text
/api/tft/player
```

### TFT Backend Files

New controller:

```text
backend/src/main/java/com/miniopgg/controller/TftController.java
```

New services:

```text
backend/src/main/java/com/miniopgg/service/TftApiService.java
backend/src/main/java/com/miniopgg/service/TftAnalyticsService.java
```

New DTOs:

```text
backend/src/main/java/com/miniopgg/dto/TftProfileResponse.java
backend/src/main/java/com/miniopgg/dto/TftRankResponse.java
backend/src/main/java/com/miniopgg/dto/TftMatchResponse.java
backend/src/main/java/com/miniopgg/dto/TftSummaryResponse.java
```

New database entities:

```text
backend/src/main/java/com/miniopgg/entity/TftPlayer.java
backend/src/main/java/com/miniopgg/entity/TftMatch.java
```

New repository:

```text
backend/src/main/java/com/miniopgg/repository/TftPlayerRepository.java
```

### TFT Database Tables

Spring creates these tables:

```text
tft_players
tft_matches
```

They are separate from the League tables:

```text
players
matches
```

This separation matters because League match data and TFT match data have different meanings. League uses champion, KDA, CS, and win/loss. TFT uses placement, level, units, traits, top 4, and first place rate.

### TFT Riot API Flow

TFT still starts with the same Riot ID lookup:

```text
Riot ID -> Account-V1 -> PUUID
```

Then TFT-specific services use:

```text
TFT Summoner-V1 -> TFT profile details
TFT League-V1 by PUUID -> TFT rank using /tft/league/v1/by-puuid/{puuid}
TFT Match-V1 -> recent TFT match history
```

For NA, the route settings are still:

```text
RIOT_REGIONAL_ROUTE=americas
RIOT_PLATFORM_ROUTE=na1
```

### TFT Summary Stats

The TFT summary calculates:

- total games analyzed
- average placement
- top 4 rate
- first place rate
- best placement
- most played unit

Top 4 rate means:

```text
matches with placement 1, 2, 3, or 4 / total matches
```

First place rate means:

```text
matches with placement 1 / total matches
```

### Why TFT Loads On Tab Click

The app searches Summoner's Rift first. TFT data loads when the TFT tab is opened.

That avoids making too many Riot calls at once. It also keeps the original League dashboard fast and stable.

### Rank Availability

Both League and TFT rank can show:

```text
UNAVAILABLE
```

That means Riot blocked the ranked endpoint for the current key or route. It does not necessarily mean the player has no rank.

If the endpoint works later, the app will try again on future searches.
