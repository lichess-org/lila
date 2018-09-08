var m = require('mithril');

function init(cfg) {
  return {
    color: m.prop(cfg.color.toLowerCase()),
    baseUrl: cfg.baseUrl,
    positions: cfg.positions,
    variant: cfg.variant,
    i18n: cfg.i18n
  };
}

function computeFen(data, cgFen) {
  return data.color().toUpperCase() + ":" + cgFen;
}

function makeUrl(url, fen) {
  return url + encodeURIComponent(fen).replace(/%20/g, '_').replace(/%2F/g, '/');
}

module.exports = {
  init: init,
  makeUrl: makeUrl,
  computeFen: computeFen
};
