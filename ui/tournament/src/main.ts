import { init, VNode, classModule, attributesModule } from 'snabbdom';
import { Chessground } from 'chessground';
import { TournamentOpts } from './interfaces';
import TournamentController from './ctrl';
import LichessChat from 'chat';

const patch = init([classModule, attributesModule]);

// eslint-disable-next-line no-duplicate-imports
import makeCtrl from './ctrl';
import view from './view/main';

export default function (opts: TournamentOpts) {
  let vnode: VNode, ctrl: TournamentController;

  $('body').data('tournament-id', opts.data.id);
  lichess.socket = new lichess.StrongSocket(`/tournament/${opts.data.id}/socket/v5`, opts.data.socketVersion, {
    receive: (t: string, d: any) => ctrl.socket.receive(t, d),
  });
  opts.socketSend = lichess.socket.send;
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
}

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
window.LichessChat = LichessChat;
