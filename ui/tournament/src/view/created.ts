import { h } from 'snabbdom'
import TournamentController from '../ctrl';
import { MaybeVNodes } from '../interfaces';
import * as pagination from '../pagination';
import { controls, standing } from './arena';
import header from './header';

export function main(ctrl: TournamentController): MaybeVNodes {
  const pag = pagination.players(ctrl);
  return [
    header(ctrl),
    controls(ctrl, pag),
    standing(ctrl, pag, 'created'),
    h('blockquote.pull-quote', [
      h('p', ctrl.data.quote.text),
      h('footer', ctrl.data.quote.author)
    ])
  ];
}

export function side(_): MaybeVNodes {
  return [];
}
