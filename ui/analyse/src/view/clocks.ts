import { h, type VNode } from 'snabbdom';
import type AnalyseCtrl from '../ctrl';
import { defined, notNull } from 'lib';
import * as licon from 'lib/licon';
import { iconTag, type MaybeVNode, type MaybeVNodes } from 'lib/view';
import { formatClockTimeVerbal } from 'lib/game/clock/clockView';
import type { TreePath } from 'lib/tree/types';

interface ClockOpts {
  centis: number | undefined;
  active: boolean;
  cls: string;
  showTenths: boolean;
  pause: boolean;
}

export default function renderClocks(ctrl: AnalyseCtrl, path: TreePath): [VNode, VNode] | undefined {
  const node = ctrl.tree.nodeAtPath(path),
    whitePov = ctrl.bottomIsWhite(),
    parentClock = ctrl.tree.getParentClock(node, path),
    isWhiteTurn = node.ply % 2 === 0,
    centis: Array<number | undefined> = (
      isWhiteTurn ? [parentClock, node.clock] : [node.clock, parentClock]
    ).map(c => (defined(c) && c < 0 ? undefined : c));

  if (!centis.some(notNull)) return;

  const study = ctrl.study;

  const lastMoveAt = study
    ? study.isClockTicking(path)
      ? study.relay?.lastMoveAt(study.vm.chapterId)
      : undefined
    : ctrl.autoplay.lastMoveAt;

  if (lastMoveAt) {
    const spent = (Date.now() - lastMoveAt) / 10;
    const i = isWhiteTurn ? 0 : 1;
    if (centis[i]) centis[i] = Math.max(0, centis[i]! - spent);
  }

  const showTenths = !study?.relay;
  const pause = !!ctrl.study?.isRelayAwayFromLive();

  return [
    renderClock({
      centis: centis[0],
      active: isWhiteTurn,
      cls: whitePov ? 'bottom' : 'top',
      showTenths,
      pause,
    }),
    renderClock({
      centis: centis[1],
      active: !isWhiteTurn,
      cls: whitePov ? 'top' : 'bottom',
      showTenths,
      pause,
    }),
  ];
}

const renderClock = (opts: ClockOpts): VNode =>
  h(
    'div.analyse__clock.' + opts.cls,
    { class: { active: opts.active } },
    site.blindMode ? [clockContentNvui(opts)] : clockContent(opts),
  );

function clockContent(opts: ClockOpts): MaybeVNodes {
  if (!opts.centis && opts.centis !== 0) return ['-'];
  const date = new Date(opts.centis * 10),
    millis = date.getUTCMilliseconds(),
    sep = ':',
    baseStr = pad2(date.getUTCMinutes()) + sep + pad2(date.getUTCSeconds());
  const timeNodes =
    !opts.showTenths || opts.centis >= 360000
      ? [Math.floor(opts.centis / 360000) + sep + baseStr]
      : opts.centis >= 6000
        ? [baseStr]
        : [baseStr, h('tenths', '.' + Math.floor(millis / 100).toString())];
  const pauseNodes = opts.pause ? [iconTag(licon.Pause)] : [];
  return [...pauseNodes, ...timeNodes];
}

function clockContentNvui(opts: ClockOpts): MaybeVNode {
  return !opts.centis && opts.centis !== 0 ? 'None' : formatClockTimeVerbal(opts.centis * 10);
}

const pad2 = (num: number): string => (num < 10 ? '0' : '') + num;
