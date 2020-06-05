var m = require('mithril');
var fenFromTag = require('draughts').fenFromTag;

function init(cfg) {
  return {
    color: m.prop(cfg.color.toLowerCase()),
    baseUrl: cfg.baseUrl,
    positions: cfg.positions,
    variant: cfg.variant,
    variants: cfg.variants,
    i18n: cfg.i18n,
    puzzleEditor: cfg.puzzleEditor
  };
}

function computeFen(data, cgFen) {
  return data.color().toUpperCase() + ":" + cgFen;
}

function makeUrl(url, fen) {
  const cleanFen = fenFromTag(fen);
  return url + encodeURIComponent(cleanFen).replace(/%20/g, '_').replace(/%2F/g, '/');
}

module.exports = {
  init: init,
  makeUrl: makeUrl,
  computeFen: computeFen
};
