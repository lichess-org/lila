var m = require('mithril');

function mapValues(o, f) {
  var n = {};
  for (var i in o) n[i] = f(o[i]);
  return n;
}

function init(cfg) {
  return {
    color: m.prop(cfg.color),
    castles: mapValues(cfg.castles, m.prop),
    baseUrl: cfg.baseUrl,
    positions: cfg.positions,
    variant: 'standard',
    i18n: cfg.i18n
  };
}

function castlesAt(v) {
  return mapValues({
    K: v,
    Q: v,
    k: v,
    q: v
  }, m.prop);
}

function fenMetadatas(data) {
  var castles = '';
  Object.keys(data.castles).forEach(function(piece) {
    if (data.castles[piece]()) castles += piece;
  });
  return data.color() + ' ' + (castles.length ? castles : '-') + ' -';
}

function computeFen(data, cgFen) {
  return cgFen + ' ' + fenMetadatas(data);
}

function makeUrl(url, fen) {
  return url + encodeURIComponent(fen).replace(/%20/g, '_').replace(/%2F/g, '/');
}

module.exports = {
  init: init,
  makeUrl: makeUrl,
  computeFen: computeFen,
  castlesAt: castlesAt
};
