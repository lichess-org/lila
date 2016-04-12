var ctrl = require('./ctrl');
var view = require('./view/main');
var m = require('mithril');

module.exports = function(opts) {

  var controller = new ctrl(opts);

  m.module(opts.element, {
    controller: function() {
      return controller;
    },
    view: view
  });

  setTimeout(function() {
    if (opts.data.game.variant.key === 'racingKings' &&
      lichess.once('round.racing-kings')) lichess.hopscotch(function() {
      hopscotch.configure({
        i18n: {
          doneBtn: 'OK, got it'
        }
      }).startTour({
        id: "round-racing-kings",
        showPrevButton: true,
        steps: [{
          title: "Racing Kings",
          content: "This is a game of racing kings. " +
            'You might want to check out <a target="_blank" href="http://lichess.org/variant/racing-kings">the rules</a>.',
          target: "div.game_infos .variant-link",
          placement: "bottom"
        }]
      });
    });
    else if (opts.data.game.variant.key === 'crazyhouse' &&
      lichess.once('round.crazyhouse')) lichess.hopscotch(function() {
      hopscotch.configure({
        i18n: {
          doneBtn: 'OK, got it'
        }
      }).startTour({
        id: "round-crazyhouse",
        showPrevButton: true,
        steps: [{
          title: "Crazyhouse",
          content: "This is a game of crazyhouse. " +
            'You might want to check out <a target="_blank" href="http://lichess.org/variant/crazyhouse">the rules</a>.',
          target: "div.game_infos .variant-link",
          placement: "bottom"
        }]
      });
    });
  }, 2000);

  return {
    socketReceive: controller.socket.receive,
    moveOn: controller.moveOn
  };
};

// lol, that's for the rest of lichess to access mithril
// without having to include it a second time
window.Chessground = require('chessground');
