#!/usr/bin/env node

import { MongoClient } from 'mongodb';

const oauthToken = process.env.OAUTH_TOKEN;
// const client = new MongoClient('mongodb://127.0.0.1:27917/lichess');
const client = new MongoClient('mongodb://127.0.0.1:27017/lichess');
const lichessUrl = 'http://l.org';
const dryRun = false;

// hardcode translations here
const translations = {
  en: `Upgrade to our new app: https://lichess.org/app`,
};

const coll = client.db().collection('lm_user_recent_nomobile');

const unsent = { sent: { $ne: true } };

const count = await coll.countDocuments(unsent);
console.log(`Sending messages to ${count} users...`);

const langs = await coll.distinct('lang', unsent);

for await (const lang of langs) {
  console.log(`Processing language ${lang}...`);

  const chunk = [];

  for await (const user of coll.find({ lang, ...unsent }).limit(100)) {
    chunk.push(user);
    if (chunk.length >= 200) {
      await processUsers(chunk, lang);
      chunk.length = 0; // Clear the chunk
    }
  }
  await processUsers(chunk, lang);
}

async function processUsers(users, lang) {
  if (users.length === 0) return;
  const ids = users.map(u => u._id);
  const message = makeMessage(lang);
  const command = `msg multi lichess ${ids.join(',')},thibault ${message}`;
  if (dryRun) console.log(command);
  else {
    const res = await fetch(`${lichessUrl}/run/cli`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${oauthToken}`,
      },
      body: command,
    });

    if (!res.ok) console.error(`Failed to send message to ${username}: ${res.status} ${res.statusText}`);
    else await coll.updateMany({ _id: { $in: ids } }, { $set: { sent: true } });
  }

  await new Promise(resolve => setTimeout(resolve, 500)); // Avoid hitting rate limits
}

function makeMessage(langCode) {
  const lang = langCode.slice(0, 2);
  return translations[lang] || translations.en;
}
