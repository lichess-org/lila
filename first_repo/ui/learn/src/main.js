var m = require('mithril');
var map = require('./map/mapMain');
var mapSide = require('./map/mapSide');
var run = require('./run/runMain');
var storage = require('./storage');

module.exports = function (element, opts) {
  opts.storage = storage(opts.data);
  delete opts.data;

  m.route.mode = 'hash';

  var trans = lichess.trans(opts.i18n);
  var side = mapSide(opts, trans);
  var sideCtrl = side.controller();

  opts.side = {
    ctrl: sideCtrl,
    view: function () {
      return side.view(sideCtrl);
    },
  };

  m.route(element, '/', {
    '/': map(opts, trans),
    '/:stage/:level': run(opts, trans),
    '/:stage': run(opts, trans),
  });

  return {};
};
