const dryRun = false;
const skip = 40 * 1000 * 1000;
const maxPerSecond = 1000;
const flushEvery = 100;
const parseLineRegex = /^([\w-~]+)([ !&\?:;])(.*)(\n|$)/;

const isChatGarbage = chat =>
  chat.l.every(l => {
    try {
      const [_, author, flag, text] = l.match(parseLineRegex);
      return isAuthorGarbage(author) || isFlagGarbage(flag) || isTextGarbage(text.trim().toLowerCase());
    } catch (e) {
      print(`${chat._id} ${e} ${l}`);
      return true;
    }
  });

// system, anon, bots
const isAuthorGarbage = author =>
  author == 'lichess' || author == 'w' || author == 'b' || author.indexOf('BOT~') == 0;

// shadowbanned and deleted
const isFlagGarbage = flag => flag == '!' || flag == '?';

const presets = new Set([
  'good luck',
  'have fun!',
  'you too!',
  'good game',
  'well played',
  'thank you',
  "i've got to go",
  'good game, well played',
]);

const isTextGarbage = text =>
  text.indexOf(' ') < 0 ||
  text.length < 9 ||
  presets.has(text) ||
  text.indexOf("I'm studying this game") == 0;

const numberFormat = n => `${Math.round(n / 1000)}k`;

let read = 0,
  deleted = 0,
  idBuffer = [];

const deleteChat = chat => {
  // if (chat.l.some(l => !l.startsWith('lichess'))) printjson(chat.l);
  // if (chat.l.some(l => l.includes('Good game'))) printjson(chat.l);
  idBuffer.push(chat._id);
  if (idBuffer.length >= flushEvery) flushBuffer();
};

const flushBuffer = () => {
  if (!dryRun) {
    db.chat.deleteMany({ _id: { $in: idBuffer } }, { writeConcern: { w: 0 } });
    sleep(Math.round((1000 * flushEvery) / maxPerSecond));
  }
  idBuffer = [];
  deleted += flushEvery;
  if (deleted % 100000 == 0)
    print(`${numberFormat(deleted)} / ${numberFormat(read)} - ${Math.round((deleted * 100) / read)}%`);
};

db.chat
  .find()
  .skip(skip)
  .forEach(chat => {
    ++read;
    if (isChatGarbage(chat)) {
      deleteChat(chat);
      if (read % 10000 == 0) printjson(chat);
    }
  });

flushBuffer();
