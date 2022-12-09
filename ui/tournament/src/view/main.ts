import { h, VNode } from 'snabbdom';
import { onInsert } from 'common/snabbdom';
import * as created from './created';
import * as started from './started';
import * as finished from './finished';
import { joinWithTeamSelector } from './battle';
import TournamentController from '../ctrl';
import { MaybeVNodes } from '../interfaces';

export default function (ctrl: TournamentController) {
  let handler: {
    name: string;
    main(ctrl: TournamentController): MaybeVNodes;
    table(ctrl: TournamentController): VNode | undefined;
  };
  if (ctrl.data.isFinished) handler = finished;
  else if (ctrl.data.isStarted) handler = started;
  else handler = created;

  return h('main.' + ctrl.opts.classes, [
    h('aside.tour__side', {
      hook: onInsert(el => {
        $(el).replaceWith(ctrl.opts.$side);
        ctrl.opts.chat && lichess.makeChat(ctrl.opts.chat);
      }),
    }),
    h('div.tour__underchat', {
      hook: onInsert(el => {
        $(el).replaceWith($('.tour__underchat.none').removeClass('none'));
      }),
    }),
    handler.table(ctrl),
    h(
      'div.tour__main',
      h(
        'div.box.' + handler.name,
        {
          class: { 'tour__main-finished': ctrl.data.isFinished },
        },
        handler.main(ctrl)
      )
    ),
    ctrl.opts.chat
      ? h('div.chat__members.none', {
          hook: onInsert(lichess.watchers),
        })
      : null,
    ctrl.joinWithTeamSelector ? joinWithTeamSelector(ctrl) : null,
  ]);
}
