import { MaybeVNodes, onInsert } from 'common/snabbdom';
import { VNode, h } from 'snabbdom';
import TournamentController from '../ctrl';
import * as pagination from '../pagination';
import { controls, standing } from './arena';
import { playing, controls as rControls, recents, standing as rStanding, yourUpcoming } from './robin';
import { teamStanding } from './battle';
import header from './header';
import teamInfo from './teamInfo';
import { arrangement } from './arrangement';

export const name = 'created';

export function main(ctrl: TournamentController): MaybeVNodes {
  const pag = pagination.players(ctrl),
    faq = ctrl.opts.$faq
      ? h('div', {
          hook: onInsert(el => $(el).replaceWith(ctrl.opts.$faq)),
        })
      : null;
  if (ctrl.isRobin())
    return [
      header(ctrl),
      rControls(ctrl),
      ...(ctrl.arrangement
        ? [arrangement(ctrl, ctrl.arrangement)]
        : [rStanding(ctrl, 'started'), yourUpcoming(ctrl), playing(ctrl), recents(ctrl)]),
      faq,
    ];
  else
    return [
      header(ctrl),
      teamStanding(ctrl, 'created'),
      controls(ctrl, pag),
      standing(ctrl, pag, 'created'),
      h(
        'blockquote.pull-quote',
        h('p', document.documentElement.lang === 'ja' ? ctrl.data.proverb.japanese : ctrl.data.proverb.english)
      ),
      faq,
    ];
}

export function table(ctrl: TournamentController): VNode | undefined {
  return ctrl.teamInfo.requested ? teamInfo(ctrl) : undefined;
}
