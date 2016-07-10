var m = require('mithril');
var map = require('./map/mapMain');
var mapSide = require('./map/mapSide');
var run = require('./run/runMain');
var storage = require('./storage');

module.exports = function(element, opts) {

  opts.storage = storage(opts.data);
  delete opts.data;

  m.route.mode = "hash";

  var side = mapSide(opts);
  var sideCtrl = side.controller();

  opts.setStage = sideCtrl.setStage;

  m.module(opts.sideElement, {
    controller: function() {
      return sideCtrl;
    },
    view: side.view
  });

  m.route(element, '/', {
    '/': map(opts),
    '/:stage/:level': run(opts),
    '/:stage': run(opts)
  });

  return {};
};
