import * as cg from 'chessground/types';
import { h, VNode } from 'snabbdom';
import { CheckCount, CheckState, MaterialDiffSide } from '../interfaces';
import { countChecks, getMaterialDiff, getScore, NO_CHECKS } from '../material';
import { opposite } from 'chessground/util';

function renderMaterialDiff(
  material: MaterialDiffSide,
  score: number,
  position: 'top' | 'bottom',
  checks?: number,
): VNode {
  const children: VNode[] = [];
  let role: cg.Role;
  for (role in material) {
    if (material[role] > 0) {
      const content: VNode[] = [];
      for (let i = 0; i < material[role]; i++) content.push(h('mpiece.' + role));
      children.push(h('div', content));
    }
  }
  if (checks) for (let i = 0; i < checks; i++) children.push(h('div', h('mpiece.king')));
  if (score > 0) children.push(h('score', '+' + score));
  return h('div.material.material-' + position, children);
}

export function renderMaterialDiffs(
  showCaptured: boolean,
  bottomColor: Color,
  fen: cg.FEN,
  showChecks: boolean,
  checkStates: CheckState[],
  ply: Ply,
): [VNode, VNode] {
  const material = getMaterialDiff(showCaptured ? fen : '');
  const score = getScore(material) * (bottomColor === 'white' ? 1 : -1);
  const checks: CheckCount = showChecks ? countChecks(checkStates, ply) : NO_CHECKS;
  const topColor = opposite(bottomColor);
  return [
    renderMaterialDiff(material[topColor], -score, 'top', checks[topColor]),
    renderMaterialDiff(material[bottomColor], score, 'bottom', checks[bottomColor]),
  ];
}
