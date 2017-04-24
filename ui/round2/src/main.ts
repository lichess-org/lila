import ctrl = require('./ctrl');
import view = require('./view/main');
import boot = require('./boot');
import * as m from 'mithril';
import { Chessground } from 'chessground';

export function main(opts) {

  var controller = new ctrl(opts);

  m.module(opts.element, {
    controller: function() {
      return controller;
    },
    view: view
  });

  return {
    socketReceive: controller.socket.receive,
    moveOn: controller.moveOn
  };
};

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;

window.onload = function() {
  if (window.lichess_round) boot(window.lichess_round, document.getElementById('lichess'));
};
