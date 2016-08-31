var m = require('mithril');

module.exports = {
  opening: function(endpoint, variant, fen, config, withGames) {
    var url;
    var params = {
      fen: fen,
      moves: 12
    };
    if (!withGames) {
      params.topGames = 0;
      params.recentGames = 0;
    }
    if (config.db.selected() === 'masters') url = '/master';
    else {
      url = '/lichess';
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
  },
  tablebase: function(endpoint, fen) {
    return m.request({
      background: true,
      method: 'GET',
      url: endpoint,
      data: {
        fen: fen
      }
    });
  }
};
