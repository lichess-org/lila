import { init, classModule, attributesModule } from 'snabbdom';
import { TournamentOpts } from './interfaces';

const patch = init([classModule, attributesModule]);

// eslint-disable-next-line no-duplicate-imports
import makeCtrl from './ctrl';
import view from './view/main';

export function initModule(opts: TournamentOpts) {
  document.body.dataset.tournamentId = opts.data.id;
  site.socket = new site.StrongSocket(`/tournament/${opts.data.id}/socket/v5`, opts.data.socketVersion, {
    receive: (t: string, d: any) => ctrl.socket.receive(t, d),
  });
  opts.socketSend = site.socket.send;
  opts.element = document.querySelector('main.tour') as HTMLElement;
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
}
