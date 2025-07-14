import { hl, type VNode, type LooseVNodes } from 'lib/snabbdom';
import { ops as treeOps } from 'lib/tree/tree';
import type AnalyseCtrl from '../ctrl';
import {
  renderInlineCommentsOf,
  renderInlineMove,
  retroLine,
  Ctx,
  Opts,
  renderingCtx,
  disclosureConnector,
  showConnector,
} from './components';

export function renderInlineView(ctrl: AnalyseCtrl): VNode {
  const ctx = renderingCtx(ctrl);
  ctrl.tree.root.collapsed = false;
  return hl('div.tview2.tview2-inline', { class: { hidden: ctrl.treeView.hidden } }, [
    renderInlineCommentsOf(ctx, ctrl.tree.root, ''),
    renderDescendantsOf(ctx, ctrl.tree.root, { parentPath: '' }),
  ]);
}

function renderDescendantsOf(ctx: Ctx, parent: Tree.Node, opts: Opts): LooseVNodes {
  const { parentPath, anchor } = opts;
  const kids = parent.children.filter(x => ctx.showComputer || !x.comp);
  if (kids.length === 0) return;
  const [main, second, third] = kids;

  if (second && !third && !treeOps.hasBranching(second, 6))
    return renderSubtree(ctx, main, { ...opts, inline: second });
  else if ((main && !second) || ctx.ctrl.idbTree.discloseOf(parent) === 'collapsed')
    return renderSubtree(ctx, main, opts);
  else
    return hl('lines', { class: { anchor: anchor === 'lines' } }, [
      anchor && disclosureConnector(),
      kids.map(
        n =>
          retroLine(ctx, n) ||
          hl('line', [hl('branch'), renderSubtree(ctx, n, { parentPath, withIndex: true })]),
      ),
    ]);
}

function renderSubtree(ctx: Ctx, node: Tree.Node, opts: Opts): LooseVNodes {
  const { parentPath, inline } = opts;
  const disclose = ctx.ctrl.idbTree.discloseOf(node);
  const path = parentPath + node.id;
  const comments = disclose !== 'collapsed' && renderInlineCommentsOf(ctx, node, path);
  return [
    renderInlineMove(ctx, node, opts),
    comments,
    inline && hl('inline', renderSubtree(ctx, inline, { parentPath, withIndex: true })),
    renderDescendantsOf(ctx, node, {
      parentPath: path,
      anchor: disclose === 'expanded' && showConnector(comments) && 'lines',
    }),
  ];
}
