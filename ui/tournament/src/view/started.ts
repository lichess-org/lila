import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import { standing } from './arena';
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
  return tour.willBePaired(ctrl) ? h('div.notice.wait',
    ctrl.trans('standByX', ctrl.data.me.username)
  ) : h('div.notice.closed', ctrl.trans('tournamentPairingsAreNowClosed'));
}

export function main(ctrl: TournamentController): MaybeVNodes {
  const gameId = ctrl.myGameId();
  return [
    header(ctrl),
    gameId ? joinTheGame(ctrl, gameId) : (tour.isIn(ctrl) ? notice(ctrl) : null),
    standing(ctrl, pagination.players(ctrl))
  ];
}

export function side(ctrl: TournamentController): MaybeVNodes {
  return ctrl.playerInfo.id ? [playerInfo(ctrl)] : tourSide(ctrl);
}
