import { h, type VNode } from 'snabbdom';

import { defined, notNull } from 'lib';
import { formatClockTimeVerbal } from 'lib/game/clock/clockView';
import * as licon from 'lib/licon';
import type { TreePath } from 'lib/tree/types';
import { iconTag, type MaybeVNode, type MaybeVNodes } from 'lib/view';

import type AnalyseCtrl from '../ctrl';

export interface SlotOpts {
  classes?: string[];
  title?: string;
  content?: VNode;
  onClick?: (e: MouseEvent) => void;
}

export interface ClockSlotOpts {
  bottom?: SlotOpts;
  top?: SlotOpts;
}

export interface ClockDataAtPath {
  whitePov: boolean;
  isWhiteTurn: boolean;
  centis: Array<number | undefined>;
  ply: number;
}

export type GetClockSlotOpts = (data: ClockDataAtPath) => ClockSlotOpts | undefined;

interface ClockOpts {
  centis: number | undefined;
  active: boolean;
  cls: string;
  showTenths: boolean;
  pause: boolean;
  slotOpts?: SlotOpts;
}

const pad2 = (num: number): string => (num < 10 ? '0' : '') + num;

export function slotNameForIndex(whitePov: boolean, index: 0 | 1): 'top' | 'bottom' {
  return index === 0 ? (whitePov ? 'bottom' : 'top') : whitePov ? 'top' : 'bottom';
}

export default function renderClocks(
  ctrl: AnalyseCtrl,
  path: TreePath,
  getClockSlotOpts?: GetClockSlotOpts,
): [VNode, VNode] | undefined {
  const node = ctrl.tree.nodeAtPath(path);
  const whitePov = ctrl.bottomIsWhite();
  const parentClock = ctrl.tree.getParentClock(node, path);
  const isWhiteTurn = node.ply % 2 === 0;
  // centis[0]/[1] are aligned with the active side-to-move index below (isWhiteTurn ? 0 : 1).
  const centis: Array<number | undefined> = (
    isWhiteTurn ? [parentClock, node.clock] : [node.clock, parentClock]
  ).map(c => (defined(c) && c < 0 ? undefined : c));

  const hasAnyClock = centis.some(notNull);
  // Compute clock data once, then pass it to getClockSlotOpts so callers (study: editable clocks)
  // can derive per-slot UI from the same values (centis/turn/pov) without duplicating this clock
  // computation anywhere else.
  const data: ClockDataAtPath = { whitePov, isWhiteTurn, centis, ply: node.ply };
  const opts = getClockSlotOpts?.(data);
  if (!hasAnyClock && !opts) return undefined;

  const study = ctrl.study;
  const lastMoveAt = study
    ? study.isClockTicking(path)
      ? study.relay?.lastMoveAt(study.vm.chapterId)
      : undefined
    : ctrl.autoplay.lastMoveAt;

  if (lastMoveAt && hasAnyClock) {
    const spent = (Date.now() - lastMoveAt) / 10;
    const i = isWhiteTurn ? 0 : 1;
    if (centis[i]) centis[i] = Math.max(0, centis[i] - spent);
  }

  const showTenths = !study?.relay;
  const pause = !!ctrl.study?.isRelayAwayFromLive();

  const slotOpts = (index: number): ClockOpts => {
    const slot = slotNameForIndex(whitePov, index as 0 | 1);
    return {
      centis: centis[index],
      active: node.ply > 0 && index === (isWhiteTurn ? 0 : 1),
      cls: slot,
      showTenths,
      pause,
      slotOpts: opts?.[slot],
    };
  };

  return [renderClock(slotOpts(0)), renderClock(slotOpts(1))];
}

const renderClock = (opts: ClockOpts): VNode => {
  const slotUi = opts.slotOpts;
  const content = slotUi?.content
    ? [slotUi.content]
    : site.blindMode
      ? [clockContentNvui(opts)]
      : clockContent(opts);

  const classList: Record<string, boolean> = { active: opts.active };
  slotUi?.classes?.forEach(c => (classList[c] = true));

  return h(
    'div.analyse__clock.' + opts.cls,
    {
      class: classList,
      attrs: slotUi?.title ? { title: slotUi.title } : {},
      on: slotUi?.onClick ? { click: slotUi.onClick } : undefined,
    },
    content,
  );
};

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

const clockContentNvui = (opts: ClockOpts): MaybeVNode =>
  !opts.centis && opts.centis !== 0 ? 'None' : formatClockTimeVerbal(opts.centis * 10);
