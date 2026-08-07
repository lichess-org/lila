import { readFileSync } from 'node:fs';

const oauthToken = process.env.OAUTH_TOKEN;
const file = process.argv[2];
const lichessUrl = 'http://l.org';
const dryRun = true;

// hardcode translations here
const translations = {
  en: `Take our 5-minute survey! As Lichess turns 16, we want to learn more about your experience to help us make Lichess better.

Please share your thoughts here: {URL}

Thanks!`,
};

// <username> <lang> <URL>
const usersAndUrls = readFileSync(file, 'utf-8')
  .split('\n')
  .map(line => line.split(' '))
  .filter(parts => parts.length === 3);

console.log(`Sending surveys to ${usersAndUrls.length} users...`);

usersAndUrls.forEach(async ([username, lang, url]) => {
  console.log(`${lang} ${username} -> ${url}`);

  const text = makeMessage(lang, url);

  if (dryRun) console.log(text);
  else {
    const res = await fetch(`${lichessUrl}/inbox/${username}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        Authorization: `Bearer ${oauthToken}`,
      },
      body: new URLSearchParams({ text }),
    });

    if (!res.ok) console.error(`Failed to send message to ${username}: ${res.status} ${res.statusText}`);
  }

  await new Promise(resolve => setTimeout(resolve, 500)); // Avoid hitting rate limits
});

function makeMessage(langCode, url) {
  const lang = langCode.slice(0, 2);
  const translated = translations[lang] || translations.en;
  return translated.replace('{URL}', url);
}
