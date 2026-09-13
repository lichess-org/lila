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
  invitation_message        Which message to send: 0, 1, 2 or 3 (defaults to 0)

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
  en: {
    0: `Take our 5-minute survey! As Lichess turns 16, we want to learn more about your experience to help us make Lichess better.

Please share your thoughts here: {URL}

Thanks!`,
    1: `Got 5 minutes to help make Lichess better?

We're looking for feedback on your experience with Lichess and how we can make it better. To help, fill out this short survey: {URL}`,
    2: `How can Lichess be better? Tell us here: {URL}

This survey takes just 5 minutes. Your answers will help us decide what to improve next.`,
    3: `Help us improve Lichess. Complete our survey: {URL}
It takes just 5 minutes.

Your answers go directly to the Lichess team.`,
  },
  ar: {
    0: `شارك في استطلاعنا الذي يستغرق ٥ دقائق! "بمناسبة مرور 16 عام على وجود Lichess، نود أن نتعرف أكثر على تجربتك لمساعدتنا في جعل Lichess أفضل."

يُرجى مشاركة آرائك هنا: {URL}

شكرًا!`,
    1: `هل لديك 5 دقائق للمساعدة في تحسين Lichess؟

نرغب في معرفة رأيك حول تجربتك مع Lichess وكيف يمكننا تحسينها. للمساعدة، يُرجى تعبئة هذا الاستبيان القصير: {URL}`,
    2: `كيف يمكن تحسين Lichess؟ أخبرنا برأيك هنا: {URL}

لن يستغرق هذا الاستبيان سوى 5 دقائق. ستساعدنا إجاباتك في تحديد ما ينبغي تحسينه بعد ذلك.`,
    3: `ساعدنا في تحسين Lichess. شارك في استبياننا: {URL}

لن يستغرق الأمر سوى 5 دقائق.

ستصل إجاباتك مباشرةً إلى فريق Lichess.`,
  },
  cs: {
    0: `Vyplň náš pětiminutový průzkum! Lichess slaví 16 let a rádi bychom se dozvěděli více o vašich zkušenostech, abychom ho mohli dále vylepšovat.

Poděl se o svůj názor zde: {URL}

Díky!`,
    1: `Máte 5 minut na to, abyste nám pomohli zlepšit Lichess?

Hledáme zpětnou vazbu ohledně vaší zkušenosti s Lichess a o tom, jak ho můžeme zlepšit. Chcete-li pomoct, vyplňte tento krátký průzkum: {URL}`,
    2: `Jak může být Lichess lepší? Řekněte nám to tady: {URL}

Tento průzkum trvá jen 5 minut. Vaše odpovědi nám pomohou rozhodnout, co budeme zlepšovat příště.`,
    3: `Pomozte nám zlepšit Lichess. Vyplňte náš průzkum: {URL}
Trvá to jen 5 minut.

Vaše odpovědi jdou přímo k týmu Lichess.`,
  },
  de: {
    0: `Beantworte unsere 5-Minuten Umfrage! Da Lichess 16 Jahre alt wird, möchten wir mehr über deine Erfahrung mit uns wissen, damit wir Lichess noch besser machen können.

Bitte teile deine Gedanken hier: {URL}

Danke!`,
    1: `Hast du 5 Minuten, um Lichess besser zu machen?

Wir suchen nach Feedback zu deiner Erfahrung mit Lichess und wie wir es verbessern können. Fülle dazu diese kurze Umfrage aus, um zu helfen: {URL}`,
    2: `Wie kann Lichess besser werden? Sag es uns hier: {URL}

Diese Umfrage dauert nur 5 Minuten. Deine Antworten werden uns helfen zu entscheiden, was wir als nächstes verbessern sollen.`,
    3: `Hilf uns, Lichess zu verbessern. Nimm an unserer Umfrage teil: {URL}
Es dauert nur 5 Minuten.

Deine Antworten gehen direkt an das Lichess-Team.`,
  },
  el: {
    0: `Βοηθήστε μας αφιερώνοντας 5 λεπτά στην έρευνά μας! Καθώς το Lichess γίνεται 16 χρονών, θα θέλαμε να μάθουμε περισσότερα για την εμπειρία σας, ώστε να το κάνουμε ακόμα καλύτερο.

Παρακαλούμε δώστε τις απόψεις σας εδώ: {URL}

Ευχαριστούμε!`,
    1: `Έχετε 5 λεπτά να μας βοηθήσετε να κάνουμε το Lichess καλύτερο;

Θα θέλαμε τα σχόλια σας σχετικά με την εμπειρία σας στο Lichess και πως θα μπορούσαμε να το κάνουμε καλύτερο. Για να μας βοηθήσετε, συμπληρώστε αυτήν την σύντομη έρευνα: {URL}`,
    2: `Πώς μπορεί το Lichess να γίνει καλύτερο; Πείτε μας εδώ: {URL}


Θα σας πάρει μόλις 5 λεπτά. Οι απαντήσεις σας θα μας βοηθήσουν να αποφασίσουμε τι πρέπει να βελτιώσουμε στο μέλλον.`,
    3: `Βοηθήστε μας να βελτιώσουμε το Lichess. Συμπληρώστε την έρευνά μας: {URL}

Δε θα σας πάρει παρά 5 λεπτά.

Οι απαντήσεις σας θα σταλούν απευθείας στην ομάδα του Lichess.`,
  },
  es: {
    0: `¡Esta encuesta solo te llevará 5 min.! Por los 16 años de Lichess, queremos conocer mejor tu experiencia para poder mejorar.

Por favor, comparte tus ideas aquí: {URL}

¡Gracias!`,
    1: `¿Tienes 5 minutos para ayudar a mejorar Lichess?

Buscamos retroalimentación en tu experiencia con Lichess y cómo mejorarlo. Para ayudar, complete esta breve encuesta: {URL}`,
    2: `¿Cómo puede mejorar Lichess? Dinos lo aquí: {URL}

Esta encuesta lleva solo 5 minutos. Tus respuestas nos ayudarán a decidir como mejorar.`,
    3: `Ayúdanos a mejorar Lichess. Completa nuestra encuesta: {URL}
Solo conlleva 5 minutos.

Tus respuestas irán directamente al equipo de Lichess.`,
  },
  fr: {
    0: `Participez à notre sondage! Ça ne prend que 5 minutes! Lichess va avoir 16 ans. Nous voulons en savoir plus sur votre expérience pour nous aider à l'améliorer.

Donnez-nous votre opinion ici : {URL}

Merci !`,
    1: `Vous avez 5 minutes pour nous aider à améliorer Lichess?

Nous souhaitons avoir vos commentaires comme utilisateur de Lichess pour nous aider à améliorer votre expérience. Nous vous invitons à remplir un court sondage : {URL}`,
    2: `Comment peut-on améliorer Lichess? Entrez vos suggestions ici : {URL}

Le sondage ne prend que 5 minutes. Vos réponses nous aideront à décider ce qu'il faut améliorer.`,
    3: `Aidez-nous à améliorer Lichess. Répondez à notre sondage : {URL}
Il ne prend que 5 minutes.

Vos réponses vont directement à l'équipe Lichess.`,
  },
  it: {
    0: `Partecipa al nostro sondaggio da 5 minuti! Mentre Lichess compie 16 anni, vogliamo sapere di più sulla tua esperienza per aiutarci a migliorare Lichess.

Per favore condividi i tuoi pensieri qui: {URL}

Grazie!`,
    1: `Hai 5 minuti per migliorare Lichess?

Vorremmo un feedback sulla tua esperienza con Lichess e su come potermmo migliorarla. Per aiutare, compila questo breve sondaggio: {URL}`,
    2: `Dove può migliorare Lichess? Raccontacelo qui: {URL}

Questo sondaggio dura solo 5 minuti. Le tue risposte ci aiuteranno a decidere cosa migliorare in futuro.`,
    3: `Aiutaci a migliorare Lichess. Completa il nostro sondaggio: {URL}
Ci vogliono solo 5 minuti.

Le tue risposte andranno direttamente al team di Lichess.`,
  },
  nl: {
    0: `Neem deel aan onze enquête van 5 minuten! Nu Lichess 16 wordt, willen we meer weten over jouw ervaring om Lichess beter te maken.

Geef uw mening hier: {URL}

Bedankt!`,
    1: `Heb je 5 minuten om Lichess beter te maken?

We zijn op zoek naar feedback over je ervaring met Lichess en hoe we het kunnen verbeteren. Vul deze korte enquête in: {URL}`,
    2: `Hoe kan Lichess beter? Vertel ons hier: {URL}

Deze enquête duurt maar 5 minuten. Je antwoorden helpen ons te bepalen wat te verbeteren.`,
    3: `Help Lichess te verbeteren. Vul onze enquête in: {URL}
Het duurt maar 5 minuten.

Je antwoorden komen direct bij het Lichess-team.`,
  },
  'pt-BR': {
    0: `Responda nossa pesquisa em 5 minutos! Enquanto o Lichess faz 16 anos, queremos aprender mais sobre a sua experiência para nos ajudar a melhorar.

Por favor, compartilhe suas ideias aqui: {URL}

Obrigado!`,
    1: `Tem 5 minutos para ajudar a melhorar Lichess?

Estamos buscando comentários sobre a sua experiência com Lichess e como nós podemos fazer ainda melhor. Para ajudar, preencha essa pesquisa curta: {URL}`,
    2: `Como Lichess poderia ser melhor? Conte para nós aqui: {URL}

Essa pesquisa leva 5 minutos. Suas respostas vão nos ajudar a decidir as próximas melhorias.`,
    3: `Nos ajude a melhorar o Lichess. Preencha a nossa pesquisa: {URL}
Leva apenas 5 minutos.

Suas respostas vão diretamente para a equipe Lichess.`,
  },
  'pt-PT': {
    0: `Responde ao nosso inquérito de 5 minutos! No momento em que o Lichess faz 16 anos, queremos saber mais sobre a tua experiência para nos ajudares a melhorar o Lichess.

Por favor, partilha os teus pensamentos aqui: {URL}

Obrigado!`,
    1: `Tens 5 minutos para ajudar a melhorar o Lichess?

Estamos à procura de feedback sobre a tua experiência com o Lichess e como o podemos melhorar. Para ajudar, preenche este questionário rápido: {URL}`,
    2: `Como pode o Lichess ser melhor? Diz-nos aqui: {URL}

Este questionário demora apenas 5 minutos. As tuas respostas vão ajudar-nos a decidir o que melhorar a seguir.`,
    3: `Ajuda-nos a melhorar o Lichess. Responde ao nosso questionário: {URL}
Demora apenas 5 minutos.

As tuas respostas vão diretamente para a equipa do Lichess.`,
  },
  ru: {
    0: `Пройдите наш 5-минутный опрос! В честь 16-летия Linchess мы хотим узнать больше о вашем опыте, чтобы сделать Linchess лучше.

Пожалуйста, поделитесь своими мыслями здесь: {URL}

Спасибо!`,
    1: `У вас есть 5 минут, чтобы помочь сделать Lichess лучше?

Нам важно знать, что вы думаете о Lichess и как мы можем его улучшить. Чтобы помочь нам, пройдите этот короткий опрос: {URL}`,
    2: `Как можно улучшить Lichess? Расскажите нам об этом: {URL}

Этот опрос займет всего 5 минут. Ваши ответы помогут нам решить, что улучшать дальше.`,
    3: `Помогите нам улучшить Lichess. Пройдите наш опрос: {URL}
Это займет всего 5 минут.

Ваши ответы поступают непосредственно в команду Lichess.`,
  },
  tr: {
    0: `5 dakikalık anketimize katılın! Lichess 16 yaşına girerken, Lichess'i daha iyi hale getirmemize yardımcı olmak için deneyimleriniz hakkında daha fazla bilgi edinmek istiyoruz.

Lütfen düşüncelerinizi burada paylaşın: {URL}

Teşekkürler!`,
    1: `Lichess'i geliştirmemize yardımcı olmak için 5 dakikanızı ayırır mısınız?

Lichess deneyiminiz ve platformu nasıl daha iyi hâle getirebileceğimiz hakkında geri bildirimlerinizi almak istiyoruz. Bize yardımcı olmak için şu kısa anketi doldurun: {URL}`,
    2: `Lichess nasıl daha iyi olabilir? Bize buradan anlatın: {URL}

Bu anket sadece 5 dakikanızı alır. Yanıtlarınız, bir sonraki adımda neleri geliştireceğimize karar vermemize yardımcı olacak.`,
    3: `Lichess'i geliştirmemize yardımcı olun. Anketimizi doldurun: {URL}
Sadece 5 dakikanızı alır.

Yanıtlarınız doğrudan Lichess ekibine iletilir.`,
  },
  vi: {
    0: `Tham gia khảo sát của chúng tôi trong 5 phút! Nhân dịp Lichess tròn 16 tuổi, chúng tôi muốn tìm hiểu thêm về trải nghiệm của bạn để giúp chúng tôi cải thiện Lichess hơn nữa.

Vui lòng chia sẻ suy nghĩ của bạn tại đây: {URL}

Xin cảm ơn!`,
    1: `Bạn có 5 phút để giúp chúng tôi cải thiện Lichess không?

Chúng tôi đang tìm kiếm phản hồi về trải nghiệm của bạn với Lichess và cách chúng tôi có thể làm tốt hơn. Để giúp đỡ, hãy điền vào khảo sát ngắn này: {URL}`,
    2: `Lichess có thể được cải thiện như thế nào? Hãy cho chúng tôi biết tại đây: {URL}

Khảo sát này chỉ mất khoảng 5 phút. Câu trả lời của bạn sẽ giúp chúng tôi quyết định nên cải thiện điều gì tiếp theo.`,
    3: `Hãy giúp chúng tôi cải thiện Lichess. Hoàn thành khảo sát của chúng tôi: {URL}
Chỉ mất 5 phút.

Câu trả lời của bạn sẽ được gửi trực tiếp đến nhóm Lichess.`,
  },
  'zh-CN': {
    0: `参与我们 5 分钟的调查！ Lichess 迎来 16 周年之际，我们希望能更深入了解您的使用体验，从而让 Lichess 变得更好。

请在此分享您的想法: {URL}

谢谢！`,
    1: `有 5 分钟时间帮助 Lichess 变得更好吗？

我们正在征集您对 Lichess 使用体验的反馈，以及我们如何能做得更好。如需提供帮助，请填写这份简短的调查问卷: {URL}`,
    2: `Lichess 如何能变得更好？在这里告诉我们: {URL}

这份问卷仅需 5 分钟。您的回答将帮助我们决定下一步改进的方向。`,
    3: `帮助我们改进 Lichess。完成我们的问卷: {URL}
只需 5 分钟。

您的回答将直接送达 Lichess 团队。`,
  },
};

const informalLangs = new Set(['cs', 'de', 'es', 'it', 'nl']);

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

  const messageId = normalizeMessageId(row.invitation_message);

  participants.push({ username, lang, token, messageId, url: surveyLink(surveyId, token, lang) });
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

for (const { username, lang, messageId, url } of participants) {
  console.log(`${lang ?? 'null'} ${username} -> ${url}`);

  const text = makeMessage(lang, messageId, url);

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

function normalizeLang(lang) {
  const value = lang?.trim();
  if (!value || value.toLowerCase() === 'null') return;
  const c = value.replaceAll('_', '-');
  const two = c.slice(0, 2);
  if (informalLangs.has(two) && (c === two || c.startsWith(`${two}-`))) return `${two}-informal`;
  return value;
}

function normalizeMessageId(value) {
  const v = value?.trim();
  return v && ['0', '1', '2', '3'].includes(v) ? v : '0';
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

function makeMessage(langCode, messageId, url) {
  return pickTranslation(langCode, messageId).replace('{URL}', url);
}

function pickTranslation(langCode, messageId) {
  const bucket = pickBucket(langCode);
  return bucket[messageId] || bucket['0'] || translations.en['0'];
}

function pickBucket(langCode) {
  if (!langCode) return translations.en;
  const c = langCode.replaceAll('_', '-');
  if (translations[c]) return translations[c];
  if (c.startsWith('pt-PT')) return translations['pt-PT'];
  if (c.startsWith('pt')) return translations['pt-BR'];
  if (c.startsWith('zh')) return translations['zh-CN'];
  return translations[c.slice(0, 2)] || translations.en;
}
