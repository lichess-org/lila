import { test, expect, type APIRequestContext } from '@playwright/test';

// The mobile app's game list filters (https://github.com/lichess-org/mobile/issues/454) read the
// profile game tabs (/@/:user/win, ...) and the game search (/games/search) as JSON, with the
// app's token. From lila-db-seed: lip_mobile_<user> tokens only have the web:mobile scope, as
// issued to the app; lip_<user> tokens have every other scope.

type Game = { id: string; winner?: 'white' | 'black'; players: Record<string, { userId?: string }> };
type Paginator = { nbResults: number; currentPageResults: Game[] };

async function getGames(request: APIRequestContext, path: string, token?: string) {
  return request.get(path, {
    headers: { Accept: 'application/json', ...(token && { Authorization: `Bearer ${token}` }) },
  });
}

async function paginator(request: APIRequestContext, path: string, token: string): Promise<Paginator> {
  const res = await getGames(request, path, token);
  expect(res.status()).toBe(200);
  return (await res.json()).paginator;
}

const winnerId = (g: Game) => g.winner && g.players[g.winner].userId;

test.describe('mobile app reads the profile game tabs', () => {
  test("lists the user's wins", async ({ request }) => {
    const pag = await paginator(request, '/@/boris/win', 'lip_mobile_boris');
    expect(pag.currentPageResults.length).toBeGreaterThan(0);
    for (const game of pag.currentPageResults) expect(winnerId(game)).toBe('boris');
  });

  test('reads the pages after the first one', async ({ request }) => {
    // Anonymous requests only get the first page.
    expect((await getGames(request, '/@/boris/all?page=2')).status()).toBe(401);
    await paginator(request, '/@/boris/all?page=2', 'lip_mobile_boris');
  });

  test('uses the search tab', async ({ request }) => {
    const pag = await paginator(request, '/@/boris/search?players.winner=boris', 'lip_mobile_boris');
    for (const game of pag.currentPageResults) expect(winnerId(game)).toBe('boris');
  });
});

test.describe('mobile app reads /games/search', () => {
  test('finds the games won by a user', async ({ request }) => {
    const pag = await paginator(
      request,
      '/games/search?players.a=boris&players.winner=boris',
      'lip_mobile_boris',
    );
    expect(pag.nbResults).toBeGreaterThan(0);
    for (const game of pag.currentPageResults) expect(winnerId(game)).toBe('boris');
  });

  test('refuses tokens without the mobile scope', async ({ request }) => {
    const res = await getGames(request, '/games/search?players.a=boris', 'lip_bobby');
    expect(res.status()).toBe(403);
  });

  test('still requires a signed-in user', async ({ request }) => {
    expect((await getGames(request, '/games/search?players.a=boris')).status()).toBe(401);
  });
});
