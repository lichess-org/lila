import { test, expect, type APIRequestContext } from '@playwright/test';

import { loginAs } from './helpers';

// From lila-db-seed: bobby leads the Private Chess Club, boris is a member, yulia isn't.
// lip_<user> tokens have every scope except web:mobile; lip_mobile_<user> tokens only have
// web:mobile, as issued to the mobile app.
const teamId = 'private-chess-club';

// One team tournament for the whole file, created by the first test of the worker.
test.describe.configure({ mode: 'default' });

let tourId: string;

test.beforeAll(async ({ request }) => {
  const res = await request.post('/api/tournament', {
    headers: { Authorization: 'Bearer lip_bobby' },
    form: {
      name: 'Club Arena',
      clockTime: 3,
      clockIncrement: 0,
      minutes: 60,
      waitMinutes: 5,
      'conditions.teamMember.teamId': teamId,
    },
  });
  expect(res.ok()).toBeTruthy();
  tourId = (await res.json()).id;
});

async function listedTourIds(request: APIRequestContext, path: string, token?: string) {
  const res = await request.get(path, {
    headers: { Accept: 'application/json', ...(token && { Authorization: `Bearer ${token}` }) },
  });
  expect(res.status()).toBe(200);
  const json = await res.json();
  return ['created', 'started', 'finished'].flatMap(k => json[k].map((t: { id: string }) => t.id));
}

test.describe('mobile app reads /tournament', () => {
  test("lists the user's team tournaments", async ({ request }) => {
    expect(await listedTourIds(request, '/tournament', 'lip_mobile_boris')).toContain(tourId);
  });

  test("doesn't list other teams' tournaments", async ({ request }) => {
    expect(await listedTourIds(request, '/tournament', 'lip_mobile_yulia')).not.toContain(tourId);
  });

  test('refuses tokens without the mobile scope', async ({ request }) => {
    const res = await request.get('/tournament', {
      headers: { Accept: 'application/json', Authorization: 'Bearer lip_bobby' },
    });
    expect(res.status()).toBe(403);
  });

  test("/api/tournament doesn't list team tournaments", async ({ request }) => {
    expect(await listedTourIds(request, '/api/tournament', 'lip_mobile_boris')).not.toContain(tourId);
  });
});

test.describe('web /tournament', () => {
  test('anonymous: no team tournaments', async ({ request }) => {
    expect(await listedTourIds(request, '/tournament')).not.toContain(tourId);
  });

  test('team member sees the team tournament in the schedule', async ({ page }) => {
    await loginAs(page, 'boris');
    await page.goto('/tournament');
    await expect(page.locator(`.tour-chart a[href="/tournament/${tourId}"]`)).toBeVisible();
  });
});
