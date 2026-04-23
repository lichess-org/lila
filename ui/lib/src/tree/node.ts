import { type Position, parseUci, makeSquare } from 'chessops';
import { chessgroundDests, lichessRules, scalachessCharPair } from 'chessops/compat';
import { parseFen } from 'chessops/fen';
import { setupPosition } from 'chessops/variant';

import { memoize } from '@/common';

import type { PositionResult, TreeNode, TreeNodeIncomplete } from './types';

// mutates and returns the node
export const completeNode =
  (variant: VariantKey) =>
  (from: TreeNodeIncomplete): TreeNode => {
    const node = from as TreeNode;
    node.id ||= node.uci ? scalachessCharPair(parseUci(node.uci)!) : '';
    node.children ||= [];
    node.pos ||= memoize(() =>
      parseFen(node.fen).chain(setup => setupPosition(lichessRules(variant), setup)),
    );
    node.dests = memoize(() => computeDests(node.pos(), variant === 'chess960'));
    node.drops = memoize(() => computeDrops(variant, node.pos()));
    node.check = memoize(() => computeCheck(node.pos()));
    node.outcome ||= memoize(() => computeOutcome(node.pos()));
    node.children.forEach(completeNode(variant));
    return node;
  };

const computeDests = (position: PositionResult, chess960: boolean) =>
  withPosition<Dests>(position, new Map(), p => chessgroundDests(p, { chess960 }));

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
