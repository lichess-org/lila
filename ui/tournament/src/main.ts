import LishogiChat from 'chat';
import { Shogiground } from 'shogiground';
import { attributesModule, classModule, init } from 'snabbdom';
import makeCtrl from './ctrl';
import { TournamentOpts } from './interfaces';
import view from './view/main';

const patch = init([classModule, attributesModule]);

export function start(opts: TournamentOpts) {
  opts.classes = opts.element.getAttribute('class');
  opts.$side = $('.tour__side').clone();
  opts.$faq = $('.tour__faq').clone();

  const ctrl = new makeCtrl(opts, redraw);

  const blueprint = view(ctrl);
  opts.element.innerHTML = '';
  let vnode = patch(opts.element, blueprint);

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  return {
    socketReceive: ctrl.socket.receive,
  };
}

// that's for the rest of lishogi to access shogiground
// without having to include it a second time
window.Shogiground = Shogiground;
window.LishogiChat = LishogiChat;
