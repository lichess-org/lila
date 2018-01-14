import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import TournamentController from '../ctrl';
import { TournamentData, MaybeVNodes } from '../interfaces';
import * as pagination from '../pagination';
import { standing, podium } from './arena';
import header from './header';
import tourSide from './side';
import playerInfo from './playerInfo';
import { numberRow } from './util';

function confetti(data: TournamentData): VNode | undefined {
  if (data.me && data.isRecentlyFinished && window.lichess.once('tournament.end.canvas.' + data.id))
  return h('canvas#confetti', {
    hook: {
      insert: _ => window.lichess.loadScript('/assets/javascripts/confetti.js')
    }
  });
}

function stats(st, noarg) {
  return h('div.stats.box', [
    h('h2', noarg('tournamentComplete')),
    h('table', [
      numberRow(noarg('averageElo'), st.averageRating, 'raw'),
      numberRow(noarg('gamesPlayed'), st.games),
      numberRow(noarg('movesPlayed'), st.moves),
      numberRow(noarg('whiteWins'), [st.whiteWins, st.games], 'percent'),
      numberRow(noarg('blackWins'), [st.blackWins, st.games], 'percent'),
      numberRow(noarg('draws'), [st.draws, st.games], 'percent'),
      numberRow(noarg('berserkRate'), [st.berserks / 2, st.games], 'percent')
    ])
  ]);
}

export function main(ctrl: TournamentController): MaybeVNodes {
  const pag = pagination.players(ctrl);
  return [
    h('div.big_top', [
      confetti(ctrl.data),
      header(ctrl),
      podium(ctrl)
    ]),
    standing(ctrl, pag)
  ];
}

export function side(ctrl: TournamentController): MaybeVNodes {
  return ctrl.playerInfo.id ? [playerInfo(ctrl)] : [
    stats ? stats(ctrl.data.stats, ctrl.trans.noarg) : null,
    ...tourSide(ctrl)
  ];
}
