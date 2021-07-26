import { getMaterialDiff, getScore, countChecks, noChecks } from '../material';
import { h, VNode } from 'snabbdom';
import { MaterialDiff, MaterialDiffSide, CheckCount, CheckState, Player } from '../interfaces';
import { Pieces } from 'chessground/types';

function renderMaterialDiff(material: MaterialDiffSide, score: number, position: 'top' | 'bottom', checks?: number) {
  const children: VNode[] = [];
  let role: string, i: number;
  for (role in material) {
    if (material[role] > 0) {
      const content: VNode[] = [];
      for (i = 0; i < material[role]; i++) content.push(h('mpiece.' + role));
      children.push(h('div', content));
    }
  }
  if (checks) for (i = 0; i < checks; i++) children.push(h('div', h('mpiece.king')));
  if (score > 0) children.push(h('score', '+' + score));
  return h('div.material.material-' + position, children);
}

const emptyMaterialDiff: MaterialDiff = {
  white: {},
  black: {},
};

export function renderMaterialDiffs(
  showCaptured: boolean,
  flip: boolean,
  player: Player,
  opponent: Player,
  pieces: Pieces,
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
  } else material = emptyMaterialDiff;

  const checks: CheckCount = player.checks || opponent.checks ? countChecks(checkStates, ply) : noChecks;

  return [
    renderMaterialDiff(material[topColor], -score, 'top', checks[topColor]),
    renderMaterialDiff(material[bottomColor], score, 'bottom', checks[bottomColor]),
  ];
}
