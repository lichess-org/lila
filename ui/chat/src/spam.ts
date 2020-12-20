import * as xhr from 'common/xhr';

export function skip(txt: string) {
  return (suspLink(txt) || followMe(txt)) && !isKnownSpammer();
}
export function selfReport(txt: string) {
  if (isKnownSpammer()) return;
  const hasSuspLink = suspLink(txt);
  if (hasSuspLink) xhr.text(
    `/jslog/${window.location.href.substr(-12)}?n=spam`,
    {method: 'post'}
  );
  if (hasSuspLink || followMe(txt))
    window.lichess.storage.set('chat-spam', '1');
}

const isKnownSpammer = () => window.lichess.storage.get('chat-spam') == '1'

const spamRegex = new RegExp([
  'xcamweb.com',
  '(^|[^i])chess-bot',
  'chess-cheat',
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
  'sexdatingcz.club'
].map(url =>
  url.replace(/\./g, '\\.').replace(/\//g, '\\/')
).join('|'));

const suspLink = (txt: string) => !!txt.match(spamRegex);
const followMe = (txt: string) => txt.toLowerCase().includes('follow me');

const teamUrlRegex = /lichess\.org\/team\//
export function hasTeamUrl(txt: string) {
  return !!txt.match(teamUrlRegex);
}
