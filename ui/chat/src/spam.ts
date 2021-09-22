export function skip(txt: string) {
  return analyse(txt) && window.lishogi.storage.get('chat-spam') != '1';
}
export function hasTeamUrl(txt: string) {
  return !!txt.match(teamUrlRegex);
}
export function report(txt: string) {
  if (analyse(txt)) {
    $.post('/jslog/' + window.location.href.substr(-12) + '?n=spam');
    window.lishogi.storage.set('chat-spam', '1');
  }
}

const spamRegex = new RegExp(
  [
    'xcamweb.com',
    '(^|[^i])shogi-bot',
    'shogi-cheat',
    'coolteenbitch',
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
    '.flinkup.com',
    '.serveusers.com',
    'badoogirls.com',
    'hide.su',
    'wyon.de',
    'sexdatingcz.club',
  ]
    .map(url => {
      return url.replace(/\./g, '\\.').replace(/\//g, '\\/');
    })
    .join('|')
);

function analyse(txt: string) {
  return !!txt.match(spamRegex);
}

const teamUrlRegex = /lishogi\.org\/team\//;
