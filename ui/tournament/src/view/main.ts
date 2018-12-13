import { h } from 'snabbdom'
import * as created from './created';
import * as started from './started';
import * as finished from './finished';
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

  return h('div#tournament.' + ctrl.opts.classes, [
    side.length ? h('div#tournament_side', side) : null,
    h('div.content_box.no_padding.tournament_box.tournament_show', {
      class: { finished: ctrl.data.isFinished }
    }, handler.main(ctrl))
  ]);
}
