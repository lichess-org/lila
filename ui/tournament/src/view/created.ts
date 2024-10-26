import { MaybeVNodes, onInsert } from 'common/snabbdom';
import { VNode, h } from 'snabbdom';
import TournamentController from '../ctrl';
import { arenaControls, robinControls, organizedControls } from './controls';
import * as pagination from '../pagination';
import { standing } from './arena';
import { standing as oStanding } from './organized';
import { playing, recents, standing as rStanding, yourUpcoming } from './robin';
import { teamStanding } from './battle';
import header from './header';
import teamInfo from './teamInfo';

export const name = 'created';

export function main(ctrl: TournamentController): MaybeVNodes {
  const pag = pagination.players(ctrl),
    proverb = h(
      'blockquote.pull-quote',
      h('p', document.documentElement.lang === 'ja' ? ctrl.data.proverb.japanese : ctrl.data.proverb.english)
    ),
    faq = ctrl.opts.$faq
      ? h('div', {
          hook: onInsert(el => $(el).replaceWith(ctrl.opts.$faq)),
        })
      : null;
  if (ctrl.isArena())
    return [
      header(ctrl),
      teamStanding(ctrl, 'created'),
      arenaControls(ctrl, pag),
      standing(ctrl, pag, 'created'),
      proverb,
      faq,
    ];
  else if (ctrl.isRobin())
    return [
      header(ctrl),
      robinControls(ctrl),
      rStanding(ctrl, 'created'),
      yourUpcoming(ctrl),
      playing(ctrl),
      recents(ctrl),
      proverb,
      faq,
    ];
  else
    return [
      header(ctrl),
      organizedControls(ctrl, pag),
      oStanding(ctrl, pag, 'created'),
      yourUpcoming(ctrl),
      playing(ctrl),
      recents(ctrl),
      proverb,
      faq,
    ];
}

export function table(ctrl: TournamentController): VNode | undefined {
  return ctrl.teamInfo.requested ? teamInfo(ctrl) : undefined;
}
