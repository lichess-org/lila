export function opening(endpoint, variant, fen, config, withGames) {
  var url;
  var params: any = {
    fen,
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
  }).then(function(data) {
    data.opening = true;
    return data;
  });
}

export function tablebase(endpoint, variant, fen) {
  return $.ajax({
    url: endpoint + '/' + variant,
    data: { fen },
    cache: true
  }).then(function(data) {
    data.tablebase = true;
    return data;
  });
}

export function watkins(endpoint, moves) {
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
