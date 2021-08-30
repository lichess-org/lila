import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import AnalyseCtrl from './ctrl';
import { isFinished } from './study/studyChapters';
import * as game from 'game';
import { defined } from '../../common/common';

export default function renderClocks(ctrl: AnalyseCtrl, withNames: boolean): [VNode, VNode] | undefined {
  if (ctrl.embed || (ctrl.data.game.status.name === 'started' && !ctrl.imported)) return;
  const node = ctrl.node,
    clock = node.clock,
    sentePov = ctrl.bottomIsSente(),
    isSenteTurn = node.ply % 2 === 0,
    showNames = withNames && !ctrl.study,
    sentePlayer = showNames ? game.getPlayer(ctrl.data, 'sente') : undefined,
    gotePlayer = showNames ? game.getPlayer(ctrl.data, 'gote') : undefined;

  if (!defined(clock) || ctrl.imported) {
    if (showNames && sentePlayer && gotePlayer && (!ctrl.synthetic || ctrl.imported))
      return [
        renderOnlyName(sentePlayer, isSenteTurn, sentePov ? 'bottom' : 'top'),
        renderOnlyName(gotePlayer, !isSenteTurn, sentePov ? 'top' : 'bottom'),
      ];
    return;
  }

  const parentClock = ctrl.tree.getParentClock(node, ctrl.path),
    centis: Array<number | undefined> = [parentClock, clock];

  if (!isSenteTurn) centis.reverse();

  const study = ctrl.study,
    relay = study && study.data.chapter.relay;
  if (relay && relay.lastMoveAt && relay.path === ctrl.path && ctrl.path !== '' && !isFinished(study!.data.chapter)) {
    const spent = (Date.now() - relay.lastMoveAt) / 10;
    const i = isSenteTurn ? 0 : 1;
    if (centis[i]) centis[i] = Math.max(0, centis[i]! - spent);
  }

  const showTenths = !ctrl.study || !ctrl.study.relay;

  return [
    renderClock(centis[0], sentePlayer, isSenteTurn, sentePov ? 'bottom' : 'top', showTenths),
    renderClock(centis[1], gotePlayer, !isSenteTurn, sentePov ? 'top' : 'bottom', showTenths),
  ];
}

function renderClock(
  centis: number | undefined,
  player: game.Player | undefined,
  active: boolean,
  cls: string,
  showTenths: boolean
): VNode {
  return h(
    'div.analyse__clock.' + cls,
    {
      class: { active },
    },
    [h('span', {}, playerName(player, true)), h('span', {}, clockContent(centis, showTenths))]
  );
}

function clockContent(centis: number | undefined, showTenths: boolean): Array<string | VNode> {
  if (!centis && centis !== 0) return ['-'];
  const date = new Date(centis * 10),
    millis = date.getUTCMilliseconds(),
    sep = ':',
    baseStr = pad2(date.getUTCMinutes()) + sep + pad2(date.getUTCSeconds());
  if (!showTenths || centis >= 360000) return [Math.floor(centis / 360000) + sep + baseStr];
  return centis >= 6000 ? [baseStr] : [baseStr, h('tenths', '.' + Math.floor(millis / 100).toString())];
}

export function renderTime(centis: number, forceHours: boolean): string {
  const hrs = Math.floor(centis / 360_000),
    mins = Math.floor(centis / 6000) % 60,
    secs = Math.floor(centis / 100) % 60,
    sep = ':';
  return ((hrs || forceHours) > 0 ? pad2(hrs) + sep : '') + pad2(mins) + sep + pad2(secs);
}

function pad2(num: number): string {
  return (num < 10 ? '0' : '') + num;
}

function playerName(player: game.Player | undefined, showSeparator: boolean): string {
  if (!player) return '';
  const sep = ' - ';
  return (
    (player.user
      ? player.user.username
      : player.ai
      ? 'Engine'
      : player.name && player.name !== '?'
      ? player.name
      : 'Anonymous') + (showSeparator ? sep : '')
  );
}

function renderOnlyName(player: game.Player, active: boolean, cls: string) {
  return h(
    'div.analyse__clock.' + cls,
    {
      class: { active },
    },
    [playerName(player, false)]
  );
}
