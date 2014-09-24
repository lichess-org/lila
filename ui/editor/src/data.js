var forIn = require('lodash-node/modern/objects/forIn')
var mapValues = require('lodash-node/modern/objects/mapValues')
var chessground = require('chessground');

function init(cfg) {
  return {
    color: m.prop(cfg.color),
    castles: mapValues(cfg.castles, m.prop),
    baseUrl: cfg.baseUrl,
    i18n: cfg.i18n
  };
}

function fenMetadatas() {
  var castles = '';
  forIn(this.castles, function(available, piece) {
    if (available()) castles += piece;
  });
  return this.color() + ' ' + (castles.length ? castles : '-');
}

function computeFen(cgBoard) {
  console.log('compute fen');
  var baseFen = chessground.fen.write(cgBoard.pieces.all);
  return baseFen + ' ' + fenMetadatas.call(this);
}

function makeUrl(fen) {
  return this.baseUrl + encodeURIComponent(fen.replace(/%20/g, '_').replace(/%2F/g, '/'));
}

function trans(key) {
  return this.i18n[key];
}

module.exports = {
  init: init,
  makeUrl: makeUrl,
  computeFen: computeFen,
  trans: trans
};
