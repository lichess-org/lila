var mapValues = require('lodash-node/modern/objects/mapValues')

function init(cfg) {
  return {
    color: m.prop(cfg.color),
    castles: mapValues(cfg.castles, m.prop),
    baseUrl: cfg.baseUrl,
    i18n: cfg.i18n
  };
}

function fenMetadatas(data) {
  var castles = '';
  Object.keys(data.castles).forEach(function(piece) {
    if (data.castles[piece]()) castles += piece;
  });
  return data.color() + ' ' + (castles.length ? castles : '-');
}

function computeFen(data, getBaseFen) {
  return getBaseFen() + ' ' + fenMetadatas(data);
}

function makeUrl(data, fen) {
  return data.baseUrl + encodeURIComponent(fen).replace(/%20/g, '_').replace(/%2F/g, '/');
}

function trans(data, key) {
  return data.i18n[key];
}

module.exports = {
  init: init,
  makeUrl: makeUrl,
  computeFen: computeFen,
  trans: trans
};
