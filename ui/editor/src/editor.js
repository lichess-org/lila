var m = require('mithril');

function mapValues(o1, f) {
  var o2 = {};
  for (var k in o1) o2[k] = f(o1[k]);
  return o2;
}

function init(cfg) {
  return {
    color: m.prop(cfg.color),
    castles: mapValues(cfg.castles, m.prop),
    baseUrl: cfg.baseUrl,
    positions: cfg.positions,
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

function computeFen(data, getBaseFen) {
  return getBaseFen() + ' ' + fenMetadatas(data);
}

function makeUrl(url, fen) {
  return url + encodeURIComponent(fen).replace(/%20/g, '_').replace(/%2F/g, '/');
}

function trans(i18n, key) {
  return i18n[key];
}

module.exports = {
  init: init,
  makeUrl: makeUrl,
  computeFen: computeFen,
  castlesAt: castlesAt,
  trans: trans
};
