var isSpammer = lichess.storage.make('spammer');

var spamRegex = new RegExp([
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
].map(function(url) {
  return url.replace(/\./g, '\\.').replace(/\//g, '\\/');
}).join('|'));

function analyse(txt) {
  return !!txt.match(spamRegex);
}

var teamUrlRegex = /lichess\.org\/team\//

module.exports = {
  hasTeamUrl: function(txt) {
    return !!txt.match(teamUrlRegex);
  },
  skip: function(txt) {
    return analyse(txt) && isSpammer.get() != '1';
  },
  report: function(txt) {
    if (analyse(txt)) {
      $.post('/jslog/____________?n=spam');
      isSpammer.set(1);
    }
  }
};
