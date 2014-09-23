var forIn = require('lodash-node/modern/objects/forIn')

function init(cfg) {
  return {
    baseUrl: cfg.baseUrl,
    color: cfg.color,
    castles: cfg.castles,
    i18n: cfg.i18n
  };
}

function fenMetadatas() {
  var castles = '';
  forIn(this.castles, function(available, piece) {
    if (available) castles += piece;
  });
  return this.color + ' ' + (castles.length ? castles : '-');
}

function makeUrl(fen) {
  return this.baseUrl + encodeURIComponent(fen.replace(/%20/g, '_').replace(/%2F/g, '/'));
}

module.exports = {
  init: init,
  makeUrl: makeUrl,
  fenMetadatas: fenMetadatas
};
