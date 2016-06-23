var m = require('mithril');
var map = require('./map/mapMain');
var stage = require('./stage/stageMain');

module.exports = function(element, opts) {

  m.route.mode = "hash";

  m.route(element, '/', {
    '/': map(opts),
    '/:id': stage(opts)
  });

  return {};
};
