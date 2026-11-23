import { prop, toggle, type Prop, type Toggle } from 'lib';
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

export default class AnalysisSession {
  readonly threatMode: Toggle = toggle(false);
  readonly promotion: PromotionCtrl;
  readonly cgConfig: Prop<CgConfig | undefined> = prop(undefined);

  constructor(redraw: Redraw, withGround: WithGround, variant?: () => VariantKey) {
    const onCancel = () => {
      const prevConfig = this.cgConfig();
      if (prevConfig) withGround(g => g.set(prevConfig));
    };
    this.promotion = new PromotionCtrl(withGround, onCancel, redraw, variant);
  }

  setPath(target: TreeCursor, tree: TreeWrapper, path: TreePath): void {
    target.path = path;
    target.nodeList = tree.getNodeList(path);
    target.node = treeOps.last(target.nodeList)!;
    target.mainline = treeOps.mainlineNodeList(tree.root);
  }
}
