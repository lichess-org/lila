export function skip(txt: string) {
  return analyse(txt) && window.lichess.storage.get('chat-spam') != '1';
}
export function hasTeamUrl(txt: string) {
  return !!txt.match(teamUrlRegex);
}
export function report(txt: string) {
  if (analyse(txt)) window.lichess.storage.set('chat-spam', '1');
}

const spamRegex = new RegExp([
  'chess-bot'
].map(url => {
  return url.replace(/\./g, '\\.').replace(/\//g, '\\/');
}).join('|'));

function analyse(txt: string) {
  return !!txt.match(spamRegex);
}

const teamUrlRegex = /lichess\.org\/team\//
