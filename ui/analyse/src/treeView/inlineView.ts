import { type VNode, type LooseVNodes, hl } from 'lib/snabbdom';
import { ops as treeOps } from 'lib/tree/tree';
import type AnalyseCtrl from '../ctrl';
import {
  type Ctx,
  type Opts,
  moveNodes,
  nodeClasses,
  renderInlineCommentsOf,
  retroLine,
  renderIndex,
  renderingCtx,
  disclosureBtn,
  disclosureConnector,
  showConnector,
} from './components';

export function renderInlineView(ctrl: AnalyseCtrl): VNode {
  const ctx: Ctx = { ...renderingCtx(ctrl) };
  return hl('div.tview2.tview2-inline', { class: { hidden: ctrl.treeView.hidden } }, [
    renderInlineCommentsOf(ctx, ctrl.tree.root, ''),
    renderDescendantsOf(ctx, ctrl.tree.root, { parentPath: '', isMainline: true }),
  ]);
}

function renderSubtree(ctx: Ctx, node: Tree.Node, opts: Opts): LooseVNodes {
  const { parentPath, isMainline } = opts;
  const path = parentPath + node.id;
  const disclose = ctx.ctrl.idbTree.discloseOf(node, isMainline);
  const comments = renderInlineCommentsOf(ctx, node, path);
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
  const branch = disclose ? parent : undefined;
  const comments = renderInlineCommentsOf(ctx, main, path).filter(Boolean); // ??
  if (main.forceVariation) {
    return [
      disclose && hl('move.empty', disclosureBtn(ctx, parent, parentPath)),
      disclose !== 'collapsed' &&
        hl(
          'interrupt',
          renderLines(ctx, [main, ...variations], {
            ...opts,
            isMainline: false,
            anchor: disclose === 'expanded' && showConnector(comments) ? 'lines' : undefined,
          }),
        ),
    ];
  }
  const maybeInterrupt = (nodes: LooseVNodes) => (disclose === 'expanded' ? hl('interrupt', nodes) : nodes);
  return [
    !disclose
      ? renderSubtree(ctx, main, { parentPath, isMainline: true, branch, inline: variations[0] })
      : [
          renderMove(ctx, main, { parentPath, isMainline: true, branch }),
          comments,
          disclose !== 'collapsed' && [
            maybeInterrupt([
              renderLines(ctx, variations, {
                parentPath,
                isMainline: disclose === 'expanded',
                anchor: disclose === 'expanded' && showConnector(comments) ? 'lines' : undefined, // TODO 'interrupt' : undefined,
              }),
            ]),
          ],
          renderDescendantsOf(ctx, main, {
            isMainline: true,
            parentPath: path,
            withIndex: disclose === 'expanded',
          }),
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
  if (second && !third && !treeOps.hasBranching(second, 6))
    return renderSubtree(ctx, main, { ...opts, inline: second });
  else if ((main && !second) || disclose === 'collapsed') return renderSubtree(ctx, main, opts);
  const comments = renderInlineCommentsOf(ctx, main, parent.id + main.id).filter(Boolean);
  const flatTail = disclose === 'expanded' && !treeOps.hasBranching(main, 6);
  return [
    flatTail ? renderSubtree(ctx, main, opts) : renderMove(ctx, main, opts),
    comments,
    renderLines(ctx, kids.slice(1), opts),
    !flatTail &&
      renderDescendantsOf(ctx, main, {
        isMainline: false,
        parentPath: opts.parentPath + main.id,
        anchor: disclose === 'expanded' && showConnector(comments) && 'lines',
        withIndex: disclose === 'expanded',
      }),
  ];
}

function renderLines(ctx: Ctx, lines: Tree.Node[], opts: Opts): LooseVNodes {
  const { parentPath, anchor } = opts;
  return hl('lines', { class: { anchor: anchor === 'lines' } }, [
    anchor && disclosureConnector(),
    lines.map(
      line =>
        retroLine(ctx, line) ||
        hl('line', [
          hl('branch'),
          renderSubtree(ctx, line, {
            parentPath,
            isMainline: false,
            withIndex: true,
          }),
        ]),
    ),
  ]);
}

function renderMove(ctx: Ctx, node: Tree.Node, opts: Opts): VNode {
  const { branch, parentPath } = opts;
  const p = parentPath + node.id;
  return hl('move', { attrs: { p }, class: { ...nodeClasses(ctx, node, p), m: opts.isMainline } }, [
    branch && disclosureBtn(ctx, branch, parentPath),
    (branch || opts.withIndex || node.ply % 2 === 1) && renderIndex(node.ply, true),
    moveNodes(ctx, node),
  ]);
}
