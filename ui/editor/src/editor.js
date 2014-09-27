var forIn = require('lodash-node/modern/objects/forIn')
var mapValues = require('lodash-node/modern/objects/mapValues')
var chessground = require('chessground');

function init(cfg) {
  return {
    color: m.prop(cfg.color),
    castles: mapValues(cfg.castles, m.prop),
    extra: {},
    baseUrl: cfg.baseUrl,
    i18n: cfg.i18n
  };
}

function fenMetadatas(data) {
  var castles = '';
  forIn(data.castles, function(available, piece) {
    if (available()) castles += piece;
  });
  return data.color() + ' ' + (castles.length ? castles : '-');
}

function computeFen(data, cgData) {
  var baseFen = chessground.fen.write(cgData.pieces);
  return baseFen + ' ' + fenMetadatas(data);
}

function makeUrl(data, fen) {
  return data.baseUrl + encodeURIComponent(fen.replace(/%20/g, '_').replace(/%2F/g, '/'));
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
