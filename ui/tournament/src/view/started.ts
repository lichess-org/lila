import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import { controls, standing } from './arena';
import header from './header';
import tourSide from './side';
import playerInfo from './playerInfo';
import { dataIcon } from './util';
import * as pagination from '../pagination';
import * as tour from '../tournament';
import TournamentController from '../ctrl';
import { MaybeVNodes } from '../interfaces';

function joinTheGame(ctrl: TournamentController, gameId: string) {
  return h('a.is.is-after.pov.button.glowed', {
    attrs: { href: '/' + gameId }
  }, [
    ctrl.trans('youArePlaying'),
    h('span.text', {
      attrs: dataIcon('G')
    }, ctrl.trans('joinTheGame'))
  ]);
}

function notice(ctrl: TournamentController): VNode {
  return tour.willBePaired(ctrl) ? h('div.tour__notice.bar-glider',
    ctrl.trans('standByX', ctrl.data.me.username)
  ) : h('div.tour__notice.closed', ctrl.trans('tournamentPairingsAreNowClosed'));
}

export function main(ctrl: TournamentController): MaybeVNodes {
  const gameId = ctrl.myGameId(),
    pag = pagination.players(ctrl);
  return [
    header(ctrl),
    gameId ? joinTheGame(ctrl, gameId) : (tour.isIn(ctrl) ? notice(ctrl) : null),
    controls(ctrl, pag),
    standing(ctrl, pag, 'created'),
  ];
}

export function side(ctrl: TournamentController): MaybeVNodes {
  return ctrl.playerInfo.id ? [playerInfo(ctrl)] : tourSide(ctrl);
}
