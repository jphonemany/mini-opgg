import { useMemo, useState } from 'react';
import { fetchPlayerDashboard, fetchTftDashboard } from './api.js';

const sampleMatches = [];
const TABS = {
  league: "Summoner's Rift",
  tft: 'TFT',
};

function App() {
  const [riotId, setRiotId] = useState('');
  const [dashboard, setDashboard] = useState(null);
  const [tftDashboard, setTftDashboard] = useState(null);
  const [status, setStatus] = useState('idle');
  const [tftStatus, setTftStatus] = useState('idle');
  const [error, setError] = useState('');
  const [tftError, setTftError] = useState('');
  const [activeTab, setActiveTab] = useState('league');
  const [searchedPlayer, setSearchedPlayer] = useState(null);

  const matches = dashboard?.matches ?? sampleMatches;
  const record = useMemo(() => {
    const wins = matches.filter((match) => match.win).length;
    return { wins, losses: matches.length - wins };
  }, [matches]);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');

    const parsed = parseRiotId(riotId);
    if (!parsed) {
      setStatus('idle');
      setError('Enter a Riot ID in the format GameName#TagLine.');
      return;
    }

    setActiveTab('league');
    setSearchedPlayer(parsed);
    setTftDashboard(null);
    setTftStatus('idle');
    setTftError('');
    setStatus('loading');
    try {
      const data = await fetchPlayerDashboard(parsed.gameName, parsed.tagLine);
      setDashboard(data);
      setStatus('success');
    } catch (requestError) {
      setDashboard(null);
      setStatus('error');
      setError(requestError.message);
    }
  }

  async function handleTabChange(tab) {
    setActiveTab(tab);
    if (tab !== 'tft' || tftDashboard || tftStatus === 'loading' || !searchedPlayer) {
      return;
    }

    setTftStatus('loading');
    setTftError('');
    try {
      const data = await fetchTftDashboard(searchedPlayer.gameName, searchedPlayer.tagLine);
      setTftDashboard(data);
      setTftStatus('success');
    } catch (requestError) {
      setTftDashboard(null);
      setTftStatus('error');
      setTftError(requestError.message);
    }
  }

  return (
    <main className="app-shell">
      <section className="search-panel">
        <div>
          <p className="eyebrow">League of Legends analytics</p>
          <h1>mini.gg</h1>
          <p className="lede">
            Search a Riot ID to view profile stats, ranked solo queue, recent
            matches, and performance trends.
          </p>
        </div>

        <form className="search-form" onSubmit={handleSubmit}>
          <label htmlFor="riot-id">Riot ID</label>
          <div className="search-row">
            <input
              id="riot-id"
              type="text"
              value={riotId}
              onChange={(event) => setRiotId(event.target.value)}
              placeholder="GameName#TagLine"
              autoComplete="off"
            />
            <button type="submit" disabled={status === 'loading'}>
              {status === 'loading' ? 'Searching' : 'Search'}
            </button>
          </div>
          {error && <p className="form-error">{error}</p>}
        </form>
      </section>

      {status === 'loading' && <LoadingDashboard />}

      {dashboard && (
        <>
          <div className="tabs" role="tablist" aria-label="Game mode">
            {Object.entries(TABS).map(([tab, label]) => (
              <button
                aria-selected={activeTab === tab}
                className={activeTab === tab ? 'active' : ''}
                key={tab}
                onClick={() => handleTabChange(tab)}
                role="tab"
                type="button"
              >
                {label}
              </button>
            ))}
          </div>

          {activeTab === 'league' && (
            <section className="dashboard-grid" aria-live="polite">
              <ProfileCard profile={dashboard.profile} record={record} />
              <RankCard rank={dashboard.rank} />
              <SummaryCards summary={dashboard.summary} />
              <MatchHistory matches={dashboard.matches} />
            </section>
          )}

          {activeTab === 'tft' && (
            <TftDashboard
              dashboard={tftDashboard}
              error={tftError}
              status={tftStatus}
            />
          )}
        </>
      )}
    </main>
  );
}

function ProfileCard({ profile, record }) {
  return (
    <article className="panel profile-card">
      <div>
        <p className="label">Player</p>
        <h2>{profile.gameName}</h2>
        <span className="tag">#{profile.tagLine}</span>
      </div>
      <div className="profile-stats">
        <Stat label="Level" value={profile.summonerLevel ?? 'N/A'} />
        <Stat label="Recent record" value={`${record.wins}W ${record.losses}L`} />
      </div>
    </article>
  );
}

function RankCard({ rank }) {
  return (
    <article className="panel rank-card">
      <p className="label">Ranked Solo</p>
      <h2>
        {rank.tier}
        {rank.rank ? ` ${rank.rank}` : ''}
      </h2>
      <div className="rank-meta">
        <span>{rank.leaguePoints} LP</span>
        <span>{rank.wins}W</span>
        <span>{rank.losses}L</span>
        <span>{rank.winRate}% WR</span>
      </div>
    </article>
  );
}

function SummaryCards({ summary }) {
  return (
    <section className="summary-strip">
      <Stat label="Games" value={summary.totalGamesAnalyzed} />
      <Stat label="Win rate" value={`${summary.recentWinRate}%`} />
      <Stat label="Avg KDA" value={summary.averageKda} />
      <Stat label="Most played" value={summary.mostPlayedChampion} />
    </section>
  );
}

function MatchHistory({ matches }) {
  return (
    <section className="panel match-panel">
      <div className="panel-heading">
        <div>
          <p className="label">Recent matches</p>
          <h2>Last {matches.length} games</h2>
        </div>
      </div>
      <div className="match-list">
        {matches.map((match) => (
          <article className={`match-row ${match.win ? 'win' : 'loss'}`} key={match.matchId}>
            <div>
              <strong>{match.championName}</strong>
              <span>{formatGameMode(match.gameMode)}</span>
            </div>
            <div className="kda">
              {match.kills} / {match.deaths} / {match.assists}
            </div>
            <div>{match.cs} CS</div>
            <div>{formatDuration(match.gameDuration)}</div>
            <div>{formatDate(match.playedAt)}</div>
            <span className="result-badge">{match.win ? 'Win' : 'Loss'}</span>
          </article>
        ))}
      </div>
    </section>
  );
}

function TftDashboard({ dashboard, error, status }) {
  if (status === 'loading') {
    return <LoadingDashboard />;
  }

  if (error) {
    return (
      <section className="dashboard-grid" aria-live="polite">
        <article className="panel full-width">
          <p className="form-error">{error}</p>
        </article>
      </section>
    );
  }

  if (!dashboard) {
    return (
      <section className="dashboard-grid" aria-live="polite">
        <article className="panel full-width">
          <p className="empty-state">Open the TFT tab to load recent TFT stats.</p>
        </article>
      </section>
    );
  }

  return (
    <section className="dashboard-grid" aria-live="polite">
      <TftRankCard rank={dashboard.rank} />
      <TftSummaryCards summary={dashboard.summary} />
      <TftMatchHistory matches={dashboard.matches} />
    </section>
  );
}

function TftRankCard({ rank }) {
  return (
    <article className="panel rank-card">
      <p className="label">TFT Ranked</p>
      <h2>
        {rank.tier}
        {rank.rank ? ` ${rank.rank}` : ''}
      </h2>
      <div className="rank-meta">
        <span>{rank.leaguePoints} LP</span>
        <span>{rank.wins}W</span>
        <span>{rank.losses}L</span>
        <span>{rank.winRate}% WR</span>
      </div>
    </article>
  );
}

function TftSummaryCards({ summary }) {
  return (
    <section className="summary-strip">
      <Stat label="Games" value={summary.totalGamesAnalyzed} />
      <Stat label="Avg place" value={summary.averagePlacement} />
      <Stat label="Top 4" value={`${summary.topFourRate}%`} />
      <Stat label="1st place" value={`${summary.firstPlaceRate}%`} />
      <Stat label="Best" value={`#${summary.bestPlacement}`} />
      <Stat label="Top unit" value={summary.mostPlayedUnit} />
    </section>
  );
}

function TftMatchHistory({ matches }) {
  return (
    <section className="panel match-panel">
      <div className="panel-heading">
        <div>
          <p className="label">Recent TFT matches</p>
          <h2>Last {matches.length} games</h2>
        </div>
      </div>
      <div className="match-list">
        {matches.map((match) => (
          <article
            className={`match-row tft ${match.placement <= 4 ? 'win' : 'loss'}`}
            key={match.matchId}
          >
            <div>
              <strong>#{match.placement}</strong>
              <span>{formatTftGameType(match.gameType)}</span>
            </div>
            <div>Level {match.level}</div>
            <div>{formatDuration(match.gameLength)}</div>
            <div>{formatDate(match.playedAt)}</div>
            <div className="wide-cell">{match.units.slice(0, 4).join(', ') || 'Units unavailable'}</div>
            <span className="result-badge">{match.placement === 1 ? '1st' : match.placement <= 4 ? 'Top 4' : 'Bot 4'}</span>
          </article>
        ))}
      </div>
    </section>
  );
}

function Stat({ label, value }) {
  return (
    <div className="stat">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function LoadingDashboard() {
  return (
    <section className="loading-panel">
      <div />
      <div />
      <div />
    </section>
  );
}

function parseRiotId(value) {
  const [gameName, tagLine, extra] = value.split('#').map((part) => part.trim());
  if (!gameName || !tagLine || extra !== undefined) {
    return null;
  }
  return { gameName, tagLine };
}

function formatDuration(seconds) {
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  return `${minutes}:${String(remainingSeconds).padStart(2, '0')}`;
}

function formatDate(value) {
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
  }).format(new Date(value));
}

function formatGameMode(gameMode) {
  const gameModes = {
    ARAM: 'ARAM',
    CLASSIC: "Summoner's Rift",
    CHERRY: 'Arena',
    TFT: 'Teamfight Tactics',
    URF: 'URF',
  };

  return gameModes[gameMode] ?? gameMode;
}

function formatTftGameType(gameType) {
  const gameTypes = {
    pairs: 'Double Up',
    standard: 'Standard',
    turbo: 'Hyper Roll',
  };

  return gameTypes[gameType] ?? gameType;
}

export default App;
