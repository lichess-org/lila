rs.secondaryOk();
const dryRun = true;
const maxPerSecond = 600;
const flushEvery = 100;
const parseLineRegex = /^([\w-~]+)([ !&\?])(.*)$/;

const isChatGarbage = chat =>
  chat.l.every(l => {
    try {
      const [_, author, flag, text] = l.match(parseLineRegex);
      return author == 'lichess' || isFlagGarbage(flag) || isTextGarbage(text.trim().toLowerCase());
    } catch (e) {
      print(e);
      print(l);
    }
  });

const isFlagGarbage = flag => flag == '!' || flag == '?';

const presets = new Set([
  'hello',
  'good luck',
  'have fun!',
  'you too!',
  'good game',
  'well played',
  'thank you',
  "i've got to go",
  'bye!',
]);

const isTextGarbage = text => text.indexOf(' ') < 0 || presets.has(text);

const numberFormat = n => `${Math.round(n / 1000)}k`;

let read = 0,
  deleted = 0,
  idBuffer = [];

const deleteChat = chat => {
  // if (chat.l.some(l => !l.startsWith('lichess'))) printjson(chat.l);
  // if (chat.l.some(l => l.includes('Good game'))) printjson(chat.l);
  idBuffer.push(chat.id);
  if (idBuffer.length >= flushEvery) flushBuffer();
};

const flushBuffer = () => {
  if (!dryRun) {
    db.chat.remove({ _id: { $in: idBuffer } }, { writeConcern: { w: 0 } });
    sleep(Math.round((1000 * flushEvery) / maxPerSecond));
  }
  idBuffer = [];
  deleted += flushEvery;
  if (deleted % 10000 == 0)
    print(`${numberFormat(deleted)} / ${numberFormat(read)} - ${Math.round((deleted * 100) / read)}%`);
};

db.chat.find().forEach(chat => {
  read++;
  if (isChatGarbage(chat)) deleteChat(chat);
});

flushBuffer();
