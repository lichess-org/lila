import { toggle, type Toggle } from 'lib';
import type { WithGround } from 'lib/game/ground';
import { PromotionCtrl } from 'lib/game/promotion';
import { treeOps, type TreeWrapper } from 'lib/tree';
import type { TreePath, TreeNode } from 'lib/tree/types';

export interface TreeCursor {
  path: TreePath;
  node: TreeNode;
  nodeList: TreeNode[];
  mainline: TreeNode[];
}

export interface AnalysisSessionDeps {
  redraw: Redraw;
  withGround: WithGround;
  cgConfig: () => CgConfig | undefined;
  variant?: () => VariantKey;
}

export interface AnalysisSession {
  threatMode: Toggle;
  promotion: PromotionCtrl;
  setPath(target: TreeCursor, tree: TreeWrapper, path: TreePath): void;
}

export function makeAnalysisSession(deps: AnalysisSessionDeps): AnalysisSession {
  return {
    threatMode: toggle(false),
    promotion: new PromotionCtrl(
      deps.withGround,
      () => deps.withGround(g => g.set(deps.cgConfig()!)),
      deps.redraw,
      deps.variant,
    ),
    setPath(target, tree, path) {
      target.path = path;
      target.nodeList = tree.getNodeList(path);
      target.node = treeOps.last(target.nodeList)!;
      target.mainline = treeOps.mainlineNodeList(tree.root);
    },
  };
}
