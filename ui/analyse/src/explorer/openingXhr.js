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
    return $.ajax({
      url: endpoint + url,
      data: params,
      cache: true
    }).then(function (data) {
      data.opening = true;
      return data;
    });
  },
  tablebase: function(endpoint, variant, fen) {
    return $.ajax({
      url: endpoint + '/' + variant,
      data: { fen: fen },
      cache: true
    }).then(function(data) {
      data.tablebase = true;
      return data;
    });
  },
  watkins: function(endpoint, moves) {
    return $.ajax({
      method: 'POST',
      url: endpoint + '/watkins',
      data: moves,
      contentType: 'text/plain'
    }).then(function(data) {
      data.watkins = true;
      return data;
    });
  }
};
