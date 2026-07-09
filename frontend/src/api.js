const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';
const REQUEST_TIMEOUT_MS = 25000;

async function getJson(path) {
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      signal: controller.signal,
    });
    const body = await response.json().catch(() => null);

    if (!response.ok) {
      throw new Error(body?.message ?? 'Request failed. Try again in a moment.');
    }

    return body;
  } catch (error) {
    if (error.name === 'AbortError') {
      throw new Error('The request took too long. Check the backend terminal for errors, then try again.');
    }
    throw error;
  } finally {
    window.clearTimeout(timeoutId);
  }
}

export async function fetchPlayerDashboard(gameName, tagLine) {
  const basePath = playerPath('/api/player', gameName, tagLine);

  const profile = await getJson(basePath);
  const [rank, matches] = await Promise.all([
    getJson(`${basePath}/rank`),
    getJson(`${basePath}/matches`),
  ]);
  const summary = await getJson(`${basePath}/summary`);

  return { profile, rank, matches, summary };
}

export async function fetchTftDashboard(gameName, tagLine) {
  const basePath = playerPath('/api/tft/player', gameName, tagLine);

  const profile = await getJson(basePath);
  const [rank, matches] = await Promise.all([
    getJson(`${basePath}/rank`),
    getJson(`${basePath}/matches`),
  ]);
  const summary = await getJson(`${basePath}/summary`);

  return { profile, rank, matches, summary };
}

function playerPath(prefix, gameName, tagLine) {
  const encodedGameName = encodeURIComponent(gameName);
  const encodedTagLine = encodeURIComponent(tagLine);
  return `${prefix}/${encodedGameName}/${encodedTagLine}`;
}
