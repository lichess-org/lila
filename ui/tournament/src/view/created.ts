import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import TournamentController from '../ctrl';
import { MaybeVNodes } from '../interfaces';
import * as pagination from '../pagination';
import { controls, standing } from './arena';
import { onInsert } from './util';
import header from './header';

export const name = 'created';

export function main(ctrl: TournamentController): MaybeVNodes {
  const pag = pagination.players(ctrl);
  return [
    header(ctrl),
    controls(ctrl, pag),
    standing(ctrl, pag, 'created'),
    h('blockquote.pull-quote', [
      h('p', ctrl.data.quote.text),
      h('footer', ctrl.data.quote.author)
    ]),
    ctrl.opts.$faq ? h('div', {
      hook: onInsert(el => $(el).replaceWith(ctrl.opts.$faq))
    }) : null
  ];
}

export function table(_: TournamentController): VNode | undefined {
  return;
}
