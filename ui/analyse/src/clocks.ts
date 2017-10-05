import AnalyseCtrl from './ctrl';
import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

interface ClockOpts {
  tenths: boolean;
}

export default function(ctrl: AnalyseCtrl): VNode | undefined {
  const node = ctrl.node, clock = node.clock;
  if (!clock && clock !== 0) return;
  const parentClock = ctrl.tree.getParentClock(node, ctrl.path);
  let whiteCentis, blackCentis;
  const isWhiteTurn = node.ply % 2 === 0;
  if (isWhiteTurn) {
    whiteCentis = parentClock;
    blackCentis = clock;
  }
  else {
    whiteCentis = clock;
    blackCentis = parentClock;
  }
  const whitePov = ctrl.bottomColor() === 'white';
  const opts = {
    tenths: !ctrl.study || !ctrl.study.relay
  };
  const whiteEl = renderClock(whiteCentis, isWhiteTurn, opts);
  const blackEl = renderClock(blackCentis, !isWhiteTurn, opts);

  return h('div.aclocks', whitePov ? [blackEl, whiteEl] : [whiteEl, blackEl]);
}

function renderClock(centis: number, active: boolean, opts: ClockOpts): VNode {
  return h('div.aclock', {
    class: { active },
  }, clockContent(centis, opts));
}

function clockContent(centis: number, opts: ClockOpts): Array<string | VNode> {
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
