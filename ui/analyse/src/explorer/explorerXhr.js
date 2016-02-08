var m = require('mithril');

var endpoint = 'http://130.211.90.176/';
// var endpoint = 'http://127.0.0.1:9000/';

module.exports = function(variant, fen, config) {
  var url;
  var params = {
    fen: fen,
    moves: 12
  };
  if (config.db.selected() === 'masters') url = 'master';
  else {
    url = 'lichess';
    params['variant'] = variant;
    params['speeds[]'] = config.speed.selected();
    params['ratings[]'] = config.rating.selected();
  }
  return m.request({
    background: true,
    method: 'GET',
    url: endpoint + url,
    data: params
  });
};
