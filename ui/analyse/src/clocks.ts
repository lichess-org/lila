import { defined } from 'common/common';
import * as game from 'game';
import * as status from 'game/status';
import { engineNameFromCode } from 'shogi/engine-name';
import { type VNode, h } from 'snabbdom';
import type AnalyseCtrl from './ctrl';

export default function renderClocks(
  ctrl: AnalyseCtrl,
  withNames: boolean,
): [VNode, VNode] | undefined {
  if (ctrl.embed || (status.started(ctrl.data) && !ctrl.imported)) return;
  const node = ctrl.node;
  const clock = node.clock;
  const sentePov = ctrl.bottomIsSente();
  const isSenteTurn = node.ply % 2 === 0;
  const showNames = withNames && !ctrl.study;
  const sentePlayer = showNames ? game.getPlayer(ctrl.data, 'sente') : undefined;
  const gotePlayer = showNames ? game.getPlayer(ctrl.data, 'gote') : undefined;

  // We are not gonna show remaining time for imported games, since it's prob not worth it for now
  if (!defined(clock) || ctrl.imported) {
    if (showNames && sentePlayer && gotePlayer && (!ctrl.synthetic || ctrl.imported))
      return [
        renderOnlyName('sente', sentePlayer, isSenteTurn, sentePov ? 'bottom' : 'top'),
        renderOnlyName('gote', gotePlayer, !isSenteTurn, sentePov ? 'top' : 'bottom'),
      ];
    return;
  }

  const parentClock = ctrl.tree.getParentClock(node, ctrl.path);
  const centis: Array<number | undefined> = [parentClock, clock];

  if (!isSenteTurn) centis.reverse();

  const showTenths = true; // let's see

  return [
    renderClock(
      'sente',
      centis[0],
      sentePlayer,
      isSenteTurn,
      sentePov ? 'bottom' : 'top',
      showTenths,
      showNames,
    ),
    renderClock(
      'gote',
      centis[1],
      gotePlayer,
      !isSenteTurn,
      sentePov ? 'top' : 'bottom',
      showTenths,
      showNames,
    ),
  ];
}

function renderClock(
  color: Color,
  centis: number | undefined,
  player: game.Player | undefined,
  active: boolean,
  cls: string,
  showTenths: boolean,
  showNames: boolean,
): VNode {
  return h(
    `div.analyse__clock.${cls}`,
    {
      class: { active },
    },
    [
      showNames ? playerName(color, player) : undefined,
      h('div.time', clockContent(centis, showTenths)),
    ],
  );
}

function clockContent(centis: number | undefined, showTenths: boolean): Array<string | VNode> {
  if (!centis && centis !== 0) return ['-'];
  const date = new Date(centis * 10);
  const millis = date.getUTCMilliseconds();
  const sep = ':';
  const baseStr = pad2(date.getUTCMinutes()) + sep + pad2(date.getUTCSeconds());
  if (!showTenths || centis >= 360000) return [Math.floor(centis / 360000) + sep + baseStr];
  return centis >= 6000
    ? [baseStr]
    : [baseStr, h('tenths', `.${Math.floor(millis / 100).toString()}`)];
}

export function renderTime(centis: number, forceHours: boolean): string {
  const hrs = Math.floor(centis / 360_000);
  const mins = Math.floor(centis / 6000) % 60;
  const secs = Math.floor(centis / 100) % 60;
  const sep = ':';
  return (hrs > 0 || forceHours ? pad2(hrs) + sep : '') + pad2(mins) + sep + pad2(secs);
}

function pad2(num: number): string {
  return (num < 10 ? '0' : '') + num;
}

function playerName(color: Color, player: game.Player | undefined): VNode {
  const name = !player
    ? ''
    : player.user
      ? player.user.username
      : player.ai
        ? engineNameFromCode(player.aiCode)
        : player.name && player.name !== '?'
          ? player.name
          : 'Anonymous';
  return h('div.name', [h(`i.color-icon.${color}`), name]);
}

function renderOnlyName(color: Color, player: game.Player, active: boolean, cls: string) {
  return h(
    `div.analyse__clock.${cls}`,
    {
      class: { active },
    },
    playerName(color, player),
  );
}
