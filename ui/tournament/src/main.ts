import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';
import { Chessground } from 'chessground';
import { TournamentOpts } from './interfaces';
import TournamentController from './ctrl';
import LichessChat from 'chat';

const patch = init([klass, attributes]);

import makeCtrl from './ctrl';
import view from './view/main';

export default function(opts: TournamentOpts) {

  let vnode: VNode, ctrl: TournamentController;

  $('body').data('tournament-id', opts.data.id);
  window.lichess.socket = new window.lichess.StrongSocket(
    `/tournament/${opts.data.id}/socket/v5`, opts.data.socketVersion, {
    receive: (t: string, d: any) => ctrl.socket.receive(t, d)
  });
  opts.socketSend = window.lichess.socket.send;
  opts.element = document.querySelector('main.tour') as HTMLElement;
  opts.classes = opts.element.getAttribute('class');
  opts.$side = $('.tour__side').clone();
  opts.$faq = $('.tour__faq').clone();

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = new makeCtrl(opts, redraw);

  const blueprint = view(ctrl);
  opts.element.innerHTML = '';
  vnode = patch(opts.element, blueprint);
};

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
window.LichessChat = LichessChat;
