var m = require('mithril');
var map = require('./map/mapMain');
var mapSide = require('./map/mapSide');
var run = require('./run/runMain');
var storage = require('./storage');

module.exports = function(element, opts) {

  opts.storage = storage(opts.data);
  delete opts.data;

  m.route.mode = "hash";

  m.route(element, '/', {
    '/': map(opts),
    '/:stage/:level': run(opts),
    '/:stage': run(opts)
  });

  m.module(opts.sideElement, mapSide(opts));

  return {};
};
