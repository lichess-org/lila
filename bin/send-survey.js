#!/usr/bin/env node

import { randomInt } from 'node:crypto';
import { readFileSync, writeFileSync } from 'node:fs';

const oauthToken = process.env.OAUTH_TOKEN;
const lichessUrl = process.env.LICHESS_URL ?? 'https://lichess.org';
const surveyId = '511794';

const { file, dryRun, writeParticipants, help } = parseArgs(process.argv.slice(2));

if (help || !file) {
  console.error(`Usage: OAUTH_TOKEN=... node send-survey.js <participants.csv> [options]

Options:
  --dry-run                 Print messages without sending PMs
  --write-participants=<path> Write a LimeSurvey import CSV for participants

The input CSV must have a header row. Required columns:
  attribute_1               Lichess username
  token                     Access code (generated if missing)

Optional columns:
  attribute_45              Lichess language/locale (omitted from survey URL if empty)

Header labels may include a parenthetical description, e.g. "attribute_1 (username)";
only the attribute_N / token name is used.

Survey links look like:
  https://lichess.org/survey?id=${surveyId}&token=<token>&lang=<lang>

Example:
  node send-survey.js participants.csv --dry-run
  node send-survey.js participants.csv --write-participants=participants_and_tokens.csv --dry-run
  OAUTH_TOKEN=... node send-survey.js participants_and_tokens.csv
`);
  process.exit(help ? 0 : 1);
}

// hardcode translations here
const translations = {
  en: `Take our 5-minute survey! As Lichess turns 16, we want to learn more about your experience to help us make Lichess better.

Please share your thoughts here: {URL}

Thanks!`,
  ar: `شارك في استطلاعنا الذي يستغرق ٥ دقائق! "بمناسبة مرور 16 عام على وجود Lichess، نود أن نتعرف أكثر على تجربتك لمساعدتنا في جعل Lichess أفضل."

يُرجى مشاركة آرائك هنا: {URL}

شكرًا!`,
  cs: `Vyplň náš pětiminutový průzkum! Lichess slaví 16 let a rádi bychom se dozvěděli více o vašich zkušenostech, abychom ho mohli dále vylepšovat.

Poděl se o svůj názor zde: {URL}

Díky!`,
  de: `Beantworte unsere 5-Minuten Umfrage! Da Lichess 16 Jahre alt wird, möchten wir mehr über deine Erfahrung mit uns wissen, damit wir Lichess noch besser machen können.

Bitte teile deine Gedanken hier: {URL}

Danke!`,
  el: `Βοηθήστε μας αφιερώνοντας 5 λεπτά στην έρευνά μας! Καθώς το Lichess γίνεται 16 χρονών, θα θέλαμε να μάθουμε περισσότερα για την εμπειρία σας, ώστε να το κάνουμε ακόμα καλύτερο.

Παρακαλούμε δώστε τις απόψεις σας εδώ: {URL}

Ευχαριστούμε!`,
  es: `¡Esta encuesta solo te llevará 5 min.! Por los 16 años de Lichess, queremos conocer mejor tu experiencia para poder mejorar.

Por favor, comparte tus ideas aquí: {URL}

¡Gracias!`,
  fr: `Participez à notre sondage! Ça ne prend que 5 minutes! Lichess va avoir 16 ans. Nous voulons en savoir plus sur votre expérience pour nous aider à l'améliorer.

Donnez-nous votre opinion ici : {URL}

Merci !`,
  it: `Partecipa al nostro sondaggio da 5 minuti! Mentre Lichess compie 16 anni, vogliamo sapere di più sulla tua esperienza per aiutarci a migliorare Lichess.

Per favore condividi i tuoi pensieri qui: {URL}

Grazie!`,
  nl: `Neem deel aan onze enquête van 5 minuten! Nu Lichess 16 wordt, willen we meer weten over jouw ervaring om Lichess beter te maken.

Geef uw mening hier: {URL}

Bedankt!`,
  'pt-BR': `Responda nossa pesquisa em 5 minutos! Enquanto o Lichess faz 16 anos, queremos aprender mais sobre a sua experiência para nos ajudar a melhorar.

Por favor, compartilhe suas ideias aqui: {URL}

Obrigado!`,
  'pt-PT': `Responde ao nosso inquérito de 5 minutos! No momento em que o Lichess faz 16 anos, queremos saber mais sobre a tua experiência para nos ajudares a melhorar o Lichess.

Por favor, partilha os teus pensamentos aqui: {URL}

Obrigado!`,
  ru: `Пройдите наш 5-минутный опрос! В честь 16-летия Linchess мы хотим узнать больше о вашем опыте, чтобы сделать Linchess лучше.

Пожалуйста, поделитесь своими мыслями здесь: {URL}

Спасибо!`,
  tr: `5 dakikalık anketimize katılın! Lichess 16 yaşına girerken, Lichess'i daha iyi hale getirmemize yardımcı olmak için deneyimleriniz hakkında daha fazla bilgi edinmek istiyoruz.

Lütfen düşüncelerinizi burada paylaşın: {URL}

Teşekkürler!`,
  vi: `Tham gia khảo sát của chúng tôi trong 5 phút! Nhân dịp Lichess tròn 16 tuổi, chúng tôi muốn tìm hiểu thêm về trải nghiệm của bạn để giúp chúng tôi cải thiện Lichess hơn nữa.

Vui lòng chia sẻ suy nghĩ của bạn tại đây: {URL}

Xin cảm ơn!`,
  'zh-CN': `参与我们 5 分钟的调查！ Lichess 迎来 16 周年之际，我们希望能更深入了解您的使用体验，从而让 Lichess 变得更好。

请在此分享您的想法: {URL}

谢谢！`,
};

const rows = parseCsv(readFileSync(file, 'utf-8'));
const usedTokens = new Set();
const participants = [];

for (const row of rows) {
  const username = row.attribute_1?.trim();
  if (!username) continue;

  const lang = normalizeLang(row.attribute_45 ?? row.attribute_4);
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

  return { file: positional[0], dryRun, writeParticipants, help };
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
    .replace(/\s*\(.*\)$/, '')
    .trim()
    .toLowerCase();
}

const informalLangs = new Set(['cs', 'de', 'es', 'it', 'nl']);

function normalizeLang(lang) {
  const value = lang?.trim();
  if (!value || value.toLowerCase() === 'null') return;
  const c = value.replaceAll('_', '-');
  const two = c.slice(0, 2);
  if (informalLangs.has(two) && (c === two || c.startsWith(`${two}-`))) return `${two}-informal`;
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
    let token = '';
    for (let i = 0; i < 15; i++) token += chars[randomInt(chars.length)];
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
  const header = 'token,attribute_1,attribute_45';
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
  return pickTranslation(langCode).replace('{URL}', url);
}

function pickTranslation(langCode) {
  if (!langCode) return translations.en;
  const c = langCode.replaceAll('_', '-');
  if (translations[c]) return translations[c];
  if (c.startsWith('pt-PT')) return translations['pt-PT'];
  if (c.startsWith('pt')) return translations['pt-BR'];
  if (c.startsWith('zh')) return translations['zh-CN'];
  return translations[c.slice(0, 2)] || translations.en;
}
