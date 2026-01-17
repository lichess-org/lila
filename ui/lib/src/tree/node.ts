import { chessgroundDests, lichessRules, scalachessCharPair } from 'chessops/compat';
import type { PositionResult, TreeNode, TreeNodeIncomplete } from './types';
import { type Position, parseUci, makeSquare } from 'chessops';
import { memoize } from '@/common';
import { parseFen } from 'chessops/fen';
import { setupPosition } from 'chessops/variant';

// mutates and returns the node
export const completeNode =
  (variant: VariantKey) =>
  (from: TreeNodeIncomplete): TreeNode => {
    const node = from as TreeNode;
    node.id ||= node.uci ? scalachessCharPair(parseUci(node.uci)!) : '';
    node.children ||= [];
    node.position ||= memoize(() =>
      parseFen(node.fen).chain(setup => setupPosition(lichessRules(variant), setup)),
    );
    node.dests = memoize(() => computeDests(node.position()));
    node.drops = memoize(() => computeDrops(variant, node.position()));
    node.check = memoize(() => computeCheck(node.position()));
    node.outcome ||= memoize(() => computeOutcome(node.position()));
    node.children.forEach(completeNode(variant));
    return node as TreeNode;
  };

const computeDests = (position: PositionResult) => withPosition(position, new Map(), chessgroundDests);

const computeDrops = (variant: VariantKey, position: PositionResult): Key[] | undefined =>
  variant === 'crazyhouse'
    ? withPosition(position, undefined, p => Array.from(p.dropDests(), makeSquare))
    : [];

const computeCheck = (position: PositionResult) => withPosition(position, false, p => p.isCheck());

const computeOutcome = (position: PositionResult) => withPosition(position, undefined, p => p.outcome());

const withPosition = <A>(position: PositionResult, defaultValue: A, f: (p: Position) => A): A =>
  position.unwrap(f, err => {
    console.error(err);
    return defaultValue;
  });
