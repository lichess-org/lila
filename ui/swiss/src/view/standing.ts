import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import SwissCtrl from '../ctrl';
import { player as renderPlayer, ratio2percent, bind, dataIcon, userName } from './util';
import { MaybeVNodes, Player } from '../interfaces';
import * as pagination from '../pagination';

const scoreTagNames = ['score', 'streak', 'double'];

function playerTr(ctrl: SwissCtrl, player: Player) {
  const userId = player.user.id;
  return h('tr', {
    key: userId,
    class: {
      me: ctrl.data.me?.id == userId,
      active: ctrl.playerInfoId === userId
    },
    hook: bind('click', _ => ctrl.showPlayerInfo(player), ctrl.redraw)
  }, [
    h('td.rank', player.absent && ctrl.data.status != 'finished' ? h('i', {
      attrs: {
        'data-icon': 'Z',
        'title': 'Absent'
      }
    }) : [player.rank]),
    h('td.player', renderPlayer(player, false, true)),
    h('td.pairings',
      h('div',
          player.sheet.map(p =>
            p == 'absent' || p == 'bye' ? h(p, p == 'absent' ? '-' : '½') :
          h('a.glpt.' + (p.o ? 'ongoing' : (p.w === true ? 'win' : (p.w === false ? 'loss' : 'draw'))), {
            attrs: {
              href: `/${p.g}`
            },
            hook: {
              insert: pairingSetup,
              postpatch(_, vnode) { pairingSetup(vnode) }
            }
          }, p.o ? '*' : (p.w === true ? '1' : (p.w === false ? '0' : '½')))
          ).concat(
          [...Array(ctrl.data.nbRounds - player.sheet.length)].map(_ => h('r'))
        )
      )),
    h('td.points', [player.points]),
    h('td.tieBreak', [player.tieBreak])
  ]);
}

const pairingSetup = (vnode: VNode) =>
  window.lichess.powertip.manualGame(vnode.elm as HTMLElement);

let lastBody: MaybeVNodes | undefined;

const preloadUserTips = (vn: VNode) => window.lichess.powertip.manualUserIn(vn.elm as HTMLElement);

export default function standing(ctrl: SwissCtrl, pag, klass?: string): VNode {
  const tableBody = pag.currentPageResults ?
    pag.currentPageResults.map(res => playerTr(ctrl, res)) : lastBody;
  if (pag.currentPageResults) lastBody = tableBody;
  return h('table.slist.swiss__standing' + (klass ? '.' + klass : ''), {
    class: {
      loading: !pag.currentPageResults,
      long: ctrl.data.round > 10,
      xlong: ctrl.data.round > 20,
    },
  }, [
    h('tbody', {
      hook: {
        insert: preloadUserTips,
        update(_, vnode) { preloadUserTips(vnode) }
      }
    }, tableBody)
  ]);
}
