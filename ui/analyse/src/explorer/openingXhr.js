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
    }).then(function (data) {
      data.opening = true;
      return data;
    });
  },
  tablebase: function(endpoint, variant, fen) {
    return m.request({
      background: true,
      method: 'GET',
      url: endpoint + '/' + variant,
      data: {
        fen: fen
      }
    }).then(function(data) {
      data.tablebase = true;
      return data;
    });
  },
  watkins: function(endpoint, moves) {
    return m.request({
      background: true,
      method: 'POST',
      url: endpoint + '/watkins',
      data: {
        moves: moves
      },
      serialize: function(data) {
        return data.moves;
      }
    }).then(function(data) {
      data.watkins = true;
      return data;
    });
  }
};
