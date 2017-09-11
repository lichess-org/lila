export function skip(txt: string) {
  return analyse(txt) && isSpammer.get() != '1';
}
export function hasTeamUrl(txt: string) {
  return !!txt.match(teamUrlRegex);
}
export function report(txt: string) {
  if (analyse(txt)) {
    $.post('/jslog/____________?n=spam');
    isSpammer.set('1');
  }
}

const isSpammer = window.lichess.storage.make('spammer');

const spamRegex = new RegExp([
  'xcamweb.com',
  'chess-bot',
  'coolteenbitch',
  'goo.gl/',
  'letcafa.webcam',
  'tinyurl.com/',
  'wooga.info/',
  'bit.ly/',
  'wbt.link/',
  'eb.by/',
  '001.rs/',
  'shr.name/',
  'u.to/',
  '.3-a.net',
  '.ssl443.org',
  '.ns02.us',
  '.myftp.info',
  '.flinkup.com'
].map(url => {
  return url.replace(/\./g, '\\.').replace(/\//g, '\\/');
}).join('|'));

const suspRegex = /\? Find me here http/;

function analyse(txt: string) {
  return !!txt.match(spamRegex) || !!txt.match(suspRegex);
}

const teamUrlRegex = /lichess\.org\/team\//
