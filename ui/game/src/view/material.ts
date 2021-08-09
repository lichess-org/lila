import * as cg from 'chessground/types';
import { h, VNode } from 'snabbdom';
import { CheckCount, CheckState, MaterialDiff, MaterialDiffSide, Player } from '../interfaces';
import { countChecks, getMaterialDiff, getScore, NO_CHECKS } from '../material';

function renderMaterialDiff(
  material: MaterialDiffSide,
  score: number,
  position: 'top' | 'bottom',
  checks?: number
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

const EMPTY_MATERIAL_DIFF: MaterialDiff = {
  white: { king: 0, queen: 0, rook: 0, bishop: 0, knight: 0, pawn: 0 },
  black: { king: 0, queen: 0, rook: 0, bishop: 0, knight: 0, pawn: 0 },
};

export function renderMaterialDiffs(
  showCaptured: boolean,
  flip: boolean,
  player: Player,
  opponent: Player,
  pieces: cg.Pieces,
  checkStates: CheckState[],
  ply: Ply
): [VNode, VNode] {
  const topColor = (flip ? player : opponent).color;
  const bottomColor = (flip ? opponent : player).color;

  let material: MaterialDiff,
    score = 0;
  if (showCaptured) {
    material = getMaterialDiff(pieces);
    score = getScore(pieces) * (bottomColor === 'white' ? 1 : -1);
  } else material = EMPTY_MATERIAL_DIFF;

  const checks: CheckCount = player.checks || opponent.checks ? countChecks(checkStates, ply) : NO_CHECKS;

  return [
    renderMaterialDiff(material[topColor], -score, 'top', checks[topColor]),
    renderMaterialDiff(material[bottomColor], score, 'bottom', checks[bottomColor]),
  ];
}
