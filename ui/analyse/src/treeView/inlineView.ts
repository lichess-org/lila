import { isEmpty } from 'lib';
import { type VNode, type LooseVNode, type LooseVNodes, hl } from 'lib/snabbdom';
import { ops as treeOps } from 'lib/tree/tree';
import type AnalyseCtrl from '../ctrl';
import {
  type Ctx,
  type Opts,
  moveNodes,
  nodeClasses,
  renderInlineCommentsOf,
  renderInlineMove,
  retroLine,
  renderIndex,
  renderingCtx,
  disclosureBtn,
  disclosureConnector,
  showConnector,
} from './components';

export function renderInlineView(ctrl: AnalyseCtrl): VNode {
  const ctx: Ctx = { ...renderingCtx(ctrl) };
  ctx.ctrl.tree.root.collapsed = false;
  return hl('div.tview2.tview2-inline', { class: { hidden: ctrl.treeView.hidden } }, [
    renderInlineCommentsOf(ctx, ctrl.tree.root, ''),
    renderDescendantsOf(ctx, ctrl.tree.root, { parentPath: '', isMainline: true }),
  ]);
}

function renderSubtree(ctx: Ctx, node: Tree.Node, opts: Opts): LooseVNodes {
  const { parentPath, isMainline } = opts;
  const path = parentPath + node.id;
  const disclose = ctx.ctrl.idbTree.discloseOf(node, isMainline);
  const comments = /*disclose !== 'collapsed' &&*/ renderInlineCommentsOf(ctx, node, path);
  if (disclose) console.log(node.ply, node.uci, 'is branch');
  return [
    renderMove(ctx, node, opts),
    comments,
    opts.inline &&
      hl('inline', renderSubtree(ctx, opts.inline, { parentPath, withIndex: true, isMainline: false })),
    renderDescendantsOf(ctx, node, {
      parentPath: path,
      isMainline,
      branch: disclose ? node : undefined,
      anchor: disclose === 'expanded' && showConnector(comments) && 'lines',
    }),
  ];
}

function renderDescendantsOf(ctx: Ctx, parent: Tree.Node, opts: Opts): LooseVNodes {
  const filteredKids = parent.children.filter(x => ctx.showComputer || !x.comp);
  if (filteredKids.length === 0) return;
  else if (opts.isMainline) return renderMainlineDescendantsOf(ctx, parent, filteredKids, opts);
  else return renderVariationDescendantsOf(ctx, parent, filteredKids, opts);
}

function renderMainlineDescendantsOf(
  ctx: Ctx,
  parent: Tree.Node,
  [main, ...variations]: Tree.Node[],
  opts: Opts,
): LooseVNodes {
  const { parentPath } = opts;
  const path = parentPath + main.id;
  const disclose = ctx.ctrl.idbTree.discloseOf(parent, !main.forceVariation);
  if (main.forceVariation) {
    return [
      disclose !== 'collapsed' &&
        hl('interrupt', renderLines(ctx, [main, ...variations], { ...opts, isMainline: false })),
    ];
  }
  const stdOpts: Opts = { parentPath, isMainline: true };
  const commentTags = renderInlineCommentsOf(ctx, main, path).filter(Boolean); // ??
  const interrupt = (nodes: LooseVNodes) => (disclose === 'expanded' ? hl('interrupt', nodes) : nodes);
  return [
    !disclose
      ? [
          renderMove(ctx, main, { ...stdOpts, branch: disclose ? parent : undefined }),
          commentTags,
          renderDescendantsOf(ctx, main, { ...stdOpts, parentPath: path }),
        ]
      : [
          renderMove(ctx, main, { ...stdOpts, branch: disclose ? parent : undefined }),
          disclose !== 'collapsed' && [
            commentTags,
            interrupt([
              renderLines(ctx, variations, {
                ...stdOpts,
                anchor: disclose === 'expanded' && showConnector(commentTags) ? 'lines' : undefined, // TODO 'interrupt' : undefined,
              }),
            ]),
          ],
          renderDescendantsOf(ctx, main, { ...stdOpts, parentPath: path }),
        ],
  ];
}

function renderVariationDescendantsOf(
  ctx: Ctx,
  parent: Tree.Node,
  kids: Tree.Node[],
  oldOpts: Opts,
): LooseVNodes {
  const [main, second, third] = kids;
  const disclose = ctx.ctrl.idbTree.discloseOf(parent);
  const opts = { ...oldOpts, branch: disclose ? parent : undefined };
  if (disclose) console.log(parent.ply, parent.uci, 'subtree variation is branch');
  if (second && !third && !treeOps.hasBranching(second, 6))
    return renderSubtree(ctx, main, { ...opts, inline: second });
  else if ((main && !second) || ctx.ctrl.idbTree.discloseOf(parent) === 'collapsed')
    return renderSubtree(ctx, main, opts);
  else return renderLines(ctx, kids, opts);
}

function renderLines(ctx: Ctx, [main, ...variations]: Tree.Node[], opts: Opts): LooseVNodes {
  const { parentPath, anchor } = opts;
  return hl('lines', { class: { anchor: anchor === 'lines' } }, [
    anchor && disclosureConnector(),
    retroLine(ctx, main) ||
      hl('line', [hl('branch'), renderSubtree(ctx, main, { ...opts, withIndex: true })]),
    variations.map(
      n =>
        retroLine(ctx, n) ||
        hl('line', [hl('branch'), renderSubtree(ctx, n, { parentPath, isMainline: false, withIndex: true })]),
    ),
  ]);
}

function renderMove(ctx: Ctx, node: Tree.Node, opts: Opts): VNode {
  const { isMainline, conceal, branch, parentPath } = opts;
  const p = parentPath + node.id;
  const classes = nodeClasses(ctx, node, p);
  if (branch) console.log(node.ply, node.uci, 'drawing disclosure button for next child');
  return hl('move', { attrs: { p }, class: classes }, [
    branch && disclosureBtn(ctx, branch, parentPath),
    (branch || opts.withIndex || node.ply % 2 === 1) && renderIndex(node.ply, true),
    moveNodes(ctx, node, opts.isMainline),
  ]);
}
