import { lichessRules, scalachessCharPair } from 'chessops/compat';
import type { TreeNode, TreeNodeIncomplete } from './types';
import { parseUci } from 'chessops';
import { memoize } from '@/common';
import { parseFen } from 'chessops/fen';
import { setupPosition } from 'chessops/variant';

// mutates and returns the node
export const completeNode =
  (variant: VariantKey) =>
  (node: TreeNodeIncomplete): TreeNode => {
    node.id ||= node.uci ? scalachessCharPair(parseUci(node.uci)!) : '';
    node.children ||= [];
    if (!node.position)
      node.position = memoize(() =>
        parseFen(node.fen).chain(setup => setupPosition(lichessRules(variant), setup)),
      );
    node.children.forEach(completeNode(variant));
    return node as TreeNode;
  };
