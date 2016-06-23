var m = require('mithril');
var map = require('./map/mapMain');
var run = require('./run/runMain');
var makeLesson = require('./lesson');

module.exports = function(element, opts) {

  m.route.mode = "hash";

  m.route(element, '/', {
    '/': map(opts),
    '/:id/:stage': run(opts),
    '/:id': run(opts)
  });

  return {};
};
