import { h } from 'snabbdom'
import * as created from './created';
import * as started from './started';
import * as finished from './finished';
import { onInsert } from './util';
import TournamentController from '../ctrl';
import { MaybeVNodes } from '../interfaces';

export default function(ctrl: TournamentController) {
  let handler: {
    main(ctrl: TournamentController): MaybeVNodes;
    side(ctrl: TournamentController): MaybeVNodes;
  };
  if (ctrl.data.isFinished) handler = finished;
  else if (ctrl.data.isStarted) handler = started;
  else handler = created;

  const side: MaybeVNodes = handler.side(ctrl);

  return h('main.' + ctrl.opts.classes, [
    h('aside.analyse__side', {
      hook: onInsert(elm => $(elm).replaceWith(ctrl.opts.$side))
    }),
    h('section.mchat', {
      hook: onInsert(_ => {
        window.lichess.makeChat(ctrl.opts.chat);
      })
    }),
    h('div.tour__underchat', {
      hook: onInsert(elm => {
        $(elm).replaceWith($('.tour__underchat.none').removeClass('none'));
      })
    }),
    ...(side.length ? side : []),
    h('div.tour__main.box', {
      class: { 'tour__main-finished': ctrl.data.isFinished }
    }, handler.main(ctrl))
  ]);
}
