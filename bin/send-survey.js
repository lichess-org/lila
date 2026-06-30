#!/usr/bin/env node

import { randomBytes } from 'node:crypto';
import { readFileSync, writeFileSync } from 'node:fs';

const oauthToken = process.env.OAUTH_TOKEN;
const lichessUrl = process.env.LICHESS_URL ?? 'https://lichess.org';

const { surveyId, file, dryRun, writeParticipants, help } = parseArgs(process.argv.slice(2));

if (help || !surveyId || !file) {
  console.error(`Usage: OAUTH_TOKEN=... node send-survey.js <surveyId> <participants.csv> [options]

Options:
  --dry-run                 Print messages without sending PMs
  --write-participants=<path> Write a LimeSurvey import CSV for participants

The input CSV must have a header row. Required columns:
  attribute_1               Lichess username
  token                     Access code (generated if missing)

Optional columns:
  attribute_4               Lichess language code (omitted from survey URL if empty)

Survey links look like:
  https://lichess.org/survey?id=<surveyId>&token=<token>&lang=<lang>

Example:
  node send-survey.js 828769 participants.csv --dry-run
  node send-survey.js 828769 participants.csv --write-participants=participants_and_tokens.csv --dry-run
  OAUTH_TOKEN=... node send-survey.js 828769 participants_and_tokens.csv
`);
  process.exit(help ? 0 : 1);
}

// hardcode translations here
const translations = {
  en: `Take our 5-minute survey! As Lichess turns 16, we want to learn more about your experience to help us make Lichess better.

Please share your thoughts here: {URL}

Thanks!`,
};

const rows = parseCsv(readFileSync(file, 'utf-8'));
const usedTokens = new Set();
const participants = [];

for (const row of rows) {
  const username = row.attribute_1?.trim();
  if (!username) continue;

  const lang = normalizeLang(row.attribute_4);
  let token = row.token?.trim();
  if (!token) token = generateToken(usedTokens);
  else validateToken(token);

  if (usedTokens.has(token)) {
    console.error(`Duplicate token ${token} for ${username}`);
    process.exit(1);
  }
  usedTokens.add(token);

  participants.push({ username, lang, token, url: surveyLink(surveyId, token, lang) });
}

if (participants.length === 0) {
  console.error('No participants found. Expected attribute_1 (username) column in CSV.');
  process.exit(1);
}

if (writeParticipants) {
  writeFileSync(writeParticipants, formatParticipantsCsv(participants));
  console.log(`Wrote ${participants.length} participants to ${writeParticipants}`);
}

console.log(`Sending surveys to ${participants.length} users...`);

for (const { username, lang, url } of participants) {
  console.log(`${lang ?? 'null'} ${username} -> ${url}`);

  const text = makeMessage(lang, url);

  if (dryRun) console.log(text);
  else {
    if (!oauthToken) {
      console.error('OAUTH_TOKEN is required unless --dry-run is set.');
      process.exit(1);
    }

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
}

// Various helpers below

function parseArgs(argv) {
  const positional = [];
  let writeParticipants;
  let dryRun = process.env.DRY_RUN === '1';
  let help = false;

  for (const arg of argv) {
    if (arg === '--dry-run') dryRun = true;
    else if (arg === '--help' || arg === '-h') help = true;
    else if (arg.startsWith('--write-participants='))
      writeParticipants = arg.slice('--write-participants='.length);
    else positional.push(arg);
  }

  return { surveyId: positional[0], file: positional[1], dryRun, writeParticipants, help };
}

function parseCsv(text) {
  const lines = text.trim().split(/\r?\n/);
  if (lines.length < 2) return [];

  const delimiter = detectDelimiter(lines[0]);
  const header = parseCsvLine(lines[0], delimiter).map(normalizeHeader);

  return lines
    .slice(1)
    .filter(line => line.trim())
    .map(line => {
      const values = parseCsvLine(line, delimiter);
      const row = {};
      header.forEach((name, i) => {
        row[name] = values[i] ?? '';
      });
      return row;
    });
}

function detectDelimiter(headerLine) {
  const commas = (headerLine.match(/,/g) || []).length;
  const semicolons = (headerLine.match(/;/g) || []).length;
  return semicolons > commas ? ';' : ',';
}

function parseCsvLine(line, delimiter) {
  const values = [];
  let current = '';
  let quoted = false;

  for (let i = 0; i < line.length; i++) {
    const ch = line[i];
    if (quoted) {
      if (ch === '"') {
        if (line[i + 1] === '"') {
          current += '"';
          i++;
        } else quoted = false;
      } else current += ch;
    } else if (ch === '"') quoted = true;
    else if (ch === delimiter) {
      values.push(current);
      current = '';
    } else current += ch;
  }

  values.push(current);
  return values;
}

function normalizeHeader(header) {
  return header
    .trim()
    .replace(/\s*<.*>$/, '')
    .trim()
    .toLowerCase();
}

function normalizeLang(lang) {
  const value = lang?.trim();
  if (!value || value.toLowerCase() === 'null') return;
  return value;
}

function validateToken(token) {
  if (!/^[A-Za-z0-9]{1,15}$/.test(token)) {
    console.error(`Invalid token "${token}": must be 1-15 alphanumeric characters.`);
    process.exit(1);
  }
}

function generateToken(usedTokens) {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  for (let attempt = 0; attempt < 100; attempt++) {
    const bytes = randomBytes(15);
    let token = '';
    for (let i = 0; i < 15; i++) token += chars[bytes[i] % chars.length];
    if (!usedTokens.has(token)) return token;
  }
  console.error('Failed to generate a unique token.');
  process.exit(1);
}

function surveyLink(id, token, lang) {
  const url = new URL('/survey', lichessUrl);
  url.searchParams.set('id', id);
  url.searchParams.set('token', token);
  if (lang) url.searchParams.set('lang', lang);
  return url.href;
}

function formatParticipantsCsv(participants) {
  const header = 'token,attribute_1,attribute_4';
  const lines = participants.map(({ token, username, lang }) =>
    [csvCell(token), csvCell(username), csvCell(lang ?? '')].join(','),
  );
  return [header, ...lines].join('\n') + '\n';
}

function csvCell(value) {
  if (/[",\n\r]/.test(value)) return `"${value.replaceAll('"', '""')}"`;
  return value;
}

function makeMessage(langCode, url) {
  const lang = langCode?.slice(0, 2) ?? 'en';
  const translated = translations[lang] || translations.en;
  return translated.replace('{URL}', url);
}
