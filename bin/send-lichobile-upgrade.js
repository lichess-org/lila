#!/usr/bin/env node

import { MongoClient } from 'mongodb';
import fs from 'node:fs';
import path from 'node:path';

async function letsGo() {
  const oauthToken = process.env.OAUTH_TOKEN;
  console.log(`Using OAuth token: ${oauthToken}`);
  const client = new MongoClient('mongodb://127.0.0.1:27917/lichess', {
    directConnection: true,
  });
  // const client = new MongoClient('mongodb://127.0.0.1:27017/lichess');
  const lichessUrl = 'https://lichess.org';
  const dryRun = false;

  const coll = client.db().collection('lm_user_recent_nomobile');

  const unsent = { sent: { $ne: true } };

  console.log('Counting users to send messages to...');
  const count = await coll.countDocuments(unsent);
  console.log(`Sending messages to ${count} users...`);

  const langs = await coll.distinct('lang', unsent);

  function findLang(from) {
    if (translations[from]) return from;
    const base = from.split('-')[0];
    if (translations[base]) {
      console.info(`Using ${base} for ${from}`);
      return base;
    }
    for (const lang of Object.keys(translations)) {
      if (lang.startsWith(base + '-')) {
        console.info(`Using ${lang} for ${from}`);
        return lang;
      }
    }
    console.warn(`No translation found for ${langCode}, using English`);
    return 'en-GB';
  }
  function makeMessage(langCode) {
    return translations[findLang(langCode)];
  }

  async function processUsers(users, lang) {
    if (users.length === 0) return;
    const ids = users.map(u => u._id);
    const message = makeMessage(lang);
    const command = `msg multi lichess ${ids.join(',')} ${message}`;
    console.log(`Sending message to ${users.length} users in ${lang}`);
    if (dryRun) {
    } else {
      const res = await fetch(`${lichessUrl}/run/cli`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${oauthToken}`,
        },
        body: command,
      });

      if (!res.ok)
        console.error(`Failed to send message to ${ids.join(',')}: ${res.status} ${res.statusText}`);
      else await coll.updateMany({ _id: { $in: ids } }, { $set: { sent: true } });
    }

    await new Promise(resolve => setTimeout(resolve, 1000)); // Avoid hitting rate limits
  }

  for await (const lang of langs) {
    console.log(`Processing language ${lang}...`);

    const chunk = [];

    for await (const user of coll.find({ lang, ...unsent })) {
      chunk.push(user);
      if (chunk.length >= 200) {
        await processUsers(chunk, lang);
        chunk.length = 0; // Clear the chunk
      }
    }
    await processUsers(chunk, lang);
  }
}

let translations = {
  'en-GB': `
To keep using Lichess on mobile, please switch to our new app: https://lichess.org/app


As Lichess evolves, our old app* is being retired and will soon stop working.
*If your Lichess app has an icon with a white background, it means you have the old app installed.

Our new app is faster, easier to use and has new features coming all the time.
If you find any issues with the new app, please post in the Lichess Feedback forum: https://lichess.org/forum/lichess-feedback


Get the new Lichess app here, totally free: https://lichess.org/app
`,
};
function loadTranslations() {
  const crowdinExportDir = '/home/thib/Downloads/lichobile-gtfo-msgs';
  const dirs = fs
    .readdirSync(crowdinExportDir, { withFileTypes: true })
    .filter(dirent => dirent.isDirectory());
  for (const dir of dirs) {
    const lang = dir.name;
    const filePath = path.join(crowdinExportDir, lang, 'Switchoff message for Lichobile.txt');
    if (fs.existsSync(filePath)) {
      const content = fs.readFileSync(filePath, 'utf8');
      translations[lang] = content.trim();
    }
  }
}
loadTranslations();
console.log(`Loaded ${Object.keys(translations).length} translations`);

await letsGo();
