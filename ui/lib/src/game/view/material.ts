import { h, type VNode } from 'snabbdom';
import type { CheckCount, CheckState, MaterialDiffSide } from '../interfaces';
import { countChecks, getMaterialDiff, getScore, NO_CHECKS } from '../material';
import { opposite } from '@lichess-org/chessground/util';
import { type Board } from 'chessops';

function renderMaterialDiff(
  material: MaterialDiffSide,
  score: number,
  position: 'top' | 'bottom',
  checks?: number,
): VNode {
  const children: VNode[] = [];
  let role: Role;
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
  chess: FEN | Board,
  showChecks: boolean,
  checkStates: CheckState[],
  ply: Ply,
): [VNode, VNode] {
  const material = getMaterialDiff(showCaptured ? chess : '');
  const score = getScore(material) * (bottomColor === 'white' ? 1 : -1);
  const checks: CheckCount = showChecks ? countChecks(checkStates, ply) : NO_CHECKS;
  const topColor = opposite(bottomColor);
  return [
    renderMaterialDiff(material[topColor], -score, 'top', checks[topColor]),
    renderMaterialDiff(material[bottomColor], score, 'bottom', checks[bottomColor]),
  ];
}
