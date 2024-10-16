import LishogiChat from 'chat';
import m from 'mithril';
import ctrl from './ctrl';
import view from './view/main';

export default function (opts) {
  var controller = new ctrl(opts);

  m.module(opts.element, {
    controller: function () {
      return controller;
    },
    view: view,
  });

  return {
    socketReceive: controller.socket.receive,
  };
}

window.LishogiChat = LishogiChat;
