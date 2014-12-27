var m = require('mithril');

var realTime = require('./module/realTime');

module.exports = function(element, env) {

  //setup routes to start w/ the `#` symbol
  m.route.mode = 'hash';

  //define a route
  m.route(element, '/', {
    '/': realTime(env)
  });
};

// lol, that's for the rest of lichess to access mithril
// without having to include it a second time
window.Chessground = require('chessground');
