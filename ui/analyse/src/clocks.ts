import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import AnalyseCtrl from './ctrl';
import { isFinished } from './study/studyChapters';

interface ClockOpts {
  tenths: boolean;
}

export default function renderClocks(ctrl: AnalyseCtrl): [VNode, VNode] | undefined {
  const node = ctrl.node, clock = node.clock;
  if (!clock && clock !== 0) return;

  const whitePov = ctrl.bottomIsWhite(),
    parentClock = ctrl.tree.getParentClock(node, ctrl.path),
    isWhiteTurn = node.ply % 2 === 0,
    centis: Array<number | undefined> = [parentClock, clock];

  if (!isWhiteTurn) centis.reverse();

  const study = ctrl.study,
    relay = study && study.data.chapter.relay;
  if (relay && relay.lastMoveAt && relay.path === ctrl.path && ctrl.path !== '' && !isFinished(study!.data.chapter)) {
    const spent = (Date.now() - relay.lastMoveAt) / 10;
    const i = isWhiteTurn ? 0 : 1;
    if (centis[i]) centis[i] = Math.max(0, centis[i]! - spent);
  }

  const opts = {
    tenths: !ctrl.study || !ctrl.study.relay
  };

  return [
    renderClock(centis[0], isWhiteTurn, whitePov ? 'bottom' : 'top', opts),
    renderClock(centis[1], !isWhiteTurn,  whitePov ? 'top' : 'bottom', opts)
  ];
}

function renderClock(centis: number | undefined, active: boolean, cls: string, opts: ClockOpts): VNode {
  return h('div.analyse__clock.' + cls, {
    class: { active },
  }, clockContent(centis, opts));
}

function clockContent(centis: number | undefined, opts: ClockOpts): Array<string | VNode> {
  if (!centis && centis !== 0) return ['-'];
  const date = new Date(centis * 10),
  millis = date.getUTCMilliseconds(),
  sep = ':',
  baseStr = pad2(date.getUTCMinutes()) + sep + pad2(date.getUTCSeconds());
  if (!opts.tenths || centis >= 360000) return [Math.floor(centis / 360000) + sep + baseStr];
  const tenthsStr = Math.floor(millis / 100).toString();
  return [
    baseStr,
    h('tenths', '.' + tenthsStr)
  ];
}

function pad2(num: number): string {
  return (num < 10 ? '0' : '') + num;
}
