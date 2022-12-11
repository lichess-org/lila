import { h, VNode } from 'snabbdom';
import { onInsert } from 'common/snabbdom';
import TournamentController from '../ctrl';
import { MaybeVNodes } from '../interfaces';
import * as pagination from '../pagination';
import { controls, standing } from './arena';
import { teamStanding } from './battle';
import teamInfo from './teamInfo';
import header from './header';

export const name = 'created';

export function main(ctrl: TournamentController): MaybeVNodes {
  const pag = pagination.players(ctrl);
  return [
    header(ctrl),
    teamStanding(ctrl, 'created'),
    controls(ctrl, pag),
    standing(ctrl, pag, 'created'),
    h('blockquote.pull-quote', [h('p', ctrl.data.quote.text), h('footer', ctrl.data.quote.author)]),
    ctrl.opts.$faq
      ? h('div', {
          hook: onInsert(el => $(el).replaceWith(ctrl.opts.$faq)),
        })
      : null,
  ];
}

export function table(ctrl: TournamentController): VNode | undefined {
  return ctrl.teamInfo.requested ? teamInfo(ctrl) : undefined;
}
