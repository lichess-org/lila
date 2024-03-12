import { h, VNode } from 'snabbdom';
import AnalyseCtrl from '../ctrl';
import { isFinished } from '../study/studyChapters';
import { notNull } from 'common';

export default function renderClocks(ctrl: AnalyseCtrl): [VNode, VNode] | undefined {
  const node = ctrl.node,
    clock = node.clock;

  const whitePov = ctrl.bottomIsWhite(),
    parentClock = ctrl.tree.getParentClock(node, ctrl.path),
    isWhiteTurn = node.ply % 2 === 0,
    centis: Array<number | undefined> = isWhiteTurn ? [parentClock, clock] : [clock, parentClock];

  if (!centis.some(notNull)) return;

  const study = ctrl.study;

  const lastMoveAt = study
    ? study.data.chapter.relayPath !== ctrl.path || ctrl.path === '' || isFinished(study.data.chapter)
      ? undefined
      : study.relay?.lastMoveAt(study.vm.chapterId)
    : ctrl.autoplay.lastMoveAt;

  if (lastMoveAt) {
    const spent = (Date.now() - lastMoveAt) / 10;
    const i = isWhiteTurn ? 0 : 1;
    if (centis[i]) centis[i] = Math.max(0, centis[i]! - spent);
  }

  const showTenths = !study?.relay;

  return [
    renderClock(centis[0], isWhiteTurn, whitePov ? 'bottom' : 'top', showTenths),
    renderClock(centis[1], !isWhiteTurn, whitePov ? 'top' : 'bottom', showTenths),
  ];
}

const renderClock = (centis: number | undefined, active: boolean, cls: string, showTenths: boolean): VNode =>
  h('div.analyse__clock.' + cls, { class: { active } }, clockContent(centis, showTenths));

function clockContent(centis: number | undefined, showTenths: boolean): Array<string | VNode> {
  if (!centis && centis !== 0) return ['-'];
  const date = new Date(centis * 10),
    millis = date.getUTCMilliseconds(),
    sep = ':',
    baseStr = pad2(date.getUTCMinutes()) + sep + pad2(date.getUTCSeconds());
  if (!showTenths || centis >= 360000) return [Math.floor(centis / 360000) + sep + baseStr];
  return centis >= 6000 ? [baseStr] : [baseStr, h('tenths', '.' + Math.floor(millis / 100).toString())];
}

const pad2 = (num: number): string => (num < 10 ? '0' : '') + num;
