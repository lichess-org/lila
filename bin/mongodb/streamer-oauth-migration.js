#!/usr/bin/env node

import { MongoClient } from 'mongodb';
import { writeFileSync } from 'node:fs';

const twitchClientId = '';
const twitchClientSecret = '';

const client = new MongoClient(
  'mongodb://127.0.0.1/lichess?directConnection=true&serverSelectionTimeoutMS=2000&appName=streamer-oauth-migrate'
);

// ===========================================================================================================

try {
  const outputPath = process.argv[2];
  if (!outputPath) throw new 'usage: streamer-oauth-migrate.mjs <output-file.ndjson>'();

  await client.connect();
  const db = client.db();
  const coll = db.collection('streamer');
  const loginToStreamerId = new Map();
  for (const doc of await coll
    .find({ 'twitch.userId': { $exists: true } }, { projection: { _id: 1, 'twitch.userId': 1 } })
    .toArray()) {
    loginToStreamerId.set(doc.twitch.userId, doc._id);
  }
  if (loginToStreamerId.size === 0) throw 'nothing to do!';

  const token = await tokenFromClientCreds();
  const updates = [];
  for (const logins of chunk([...loginToStreamerId.keys()])) {
    for (const { login, id } of await fetchTwitchUsers(logins, token)) {
      updates.push({ _id: loginToStreamerId.get(login), twitch: { login, id } });
    }
  }

  const ndjson = updates.map(op => JSON.stringify(op)).join('\n') + '\n';
  writeFileSync(outputPath, ndjson);

  console.log(`wrote ${updates.length} updates`);
} catch (err) {
  console.error(err);
  process.exit(1);
} finally {
  await client.close().catch(() => {});
}

// ===========================================================================================================

function chunk(array) {
  const out = [];
  for (let i = 0; i < array.length; i += 100) out.push(array.slice(i, i + 100));
  return out;
}

async function fetchTwitchUsers(logins, token) {
  const params = new URLSearchParams();
  for (const login of logins) params.append('login', login);

  const json = await fetch(`https://api.twitch.tv/helix/users?${params.toString()}`, {
    method: 'GET',
    headers: { 'Client-Id': twitchClientId, Authorization: `Bearer ${token}` },
  }).then(rsp => rsp.json());

  return Array.isArray(json.data) ? json.data : [];
}

async function tokenFromClientCreds() {
  const params = new URLSearchParams();
  params.append('client_id', twitchClientId);
  params.append('client_secret', twitchClientSecret);
  params.append('grant_type', 'client_credentials');
  const rsp = await fetch(`https://id.twitch.tv/oauth2/token?${params.toString()}`, { method: 'POST' });
  return (await rsp.json()).access_token;
}
