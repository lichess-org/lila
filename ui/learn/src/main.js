var m = require('mithril');
var map = require('./map/mapMain');
var mapSide = require('./map/mapSide');
var run = require('./run/runMain');

module.exports = function(element, opts) {

  m.route.mode = "hash";

  m.route(element, '/', {
    '/': map(opts),
    '/:id/:level': run(opts),
    '/:id': run(opts)
  });

  m.module(opts.sideElement, mapSide(opts));

  return {};
};
