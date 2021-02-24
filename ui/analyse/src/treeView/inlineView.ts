import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import { fixCrazySan } from 'chess';
import { path as treePath, ops as treeOps } from 'tree';
import * as moveView from '../moveView';
import AnalyseCtrl from '../ctrl';
import { MaybeVNodes } from '../interfaces';
import { mainHook, nodeClasses, findCurrentPath, renderInlineCommentsOf, retroLine } from './treeView';
import { Ctx, Opts } from './treeView';

function renderChildrenOf(ctx: Ctx, node: Tree.Node, opts: Opts): MaybeVNodes | undefined {
  const cs = node.children,
    main = cs[0];
  if (!main) return;
  if (opts.isMainline) {
    if (!cs[1] && !main.forceVariation)
      return renderMoveAndChildrenOf(ctx, main, {
        parentPath: opts.parentPath,
        isMainline: true,
        withIndex: opts.withIndex,
      });
    return (
      renderInlined(ctx, cs, opts) || [
        ...(main.forceVariation
          ? []
          : [
              renderMoveOf(ctx, main, {
                parentPath: opts.parentPath,
                isMainline: true,
                withIndex: opts.withIndex,
              }),
              ...renderInlineCommentsOf(ctx, main),
            ]),
        h(
          'interrupt',
          renderLines(ctx, main.forceVariation ? cs : cs.slice(1), {
            parentPath: opts.parentPath,
            isMainline: true,
          })
        ),
        ...(main.forceVariation
          ? []
          : renderChildrenOf(ctx, main, {
              parentPath: opts.parentPath + main.id,
              isMainline: true,
              withIndex: true,
            }) || []),
      ]
    );
  }
  if (!cs[1]) return renderMoveAndChildrenOf(ctx, main, opts);
  return renderInlined(ctx, cs, opts) || [renderLines(ctx, cs, opts)];
}

function renderInlined(ctx: Ctx, nodes: Tree.Node[], opts: Opts): MaybeVNodes | undefined {
  // only 2 branches
  if (!nodes[1] || nodes[2] || nodes[0].forceVariation) return;
  // only if second branch has no sub-branches
  if (treeOps.hasBranching(nodes[1], 6)) return;
  return renderMoveAndChildrenOf(ctx, nodes[0], {
    parentPath: opts.parentPath,
    isMainline: opts.isMainline,
    inline: nodes[1],
  });
}

function renderLines(ctx: Ctx, nodes: Tree.Node[], opts: Opts): VNode {
  return h(
    'lines',
    nodes.map(n => {
      return (
        retroLine(ctx, n, opts) ||
        h(
          'line',
          renderMoveAndChildrenOf(ctx, n, {
            parentPath: opts.parentPath,
            isMainline: false,
            withIndex: true,
            truncate: n.comp && !treePath.contains(ctx.ctrl.path, opts.parentPath + n.id) ? 3 : undefined,
          })
        )
      );
    })
  );
}

function renderMoveAndChildrenOf(ctx: Ctx, node: Tree.Node, opts: Opts): MaybeVNodes {
  const path = opts.parentPath + node.id,
    comments = renderInlineCommentsOf(ctx, node);
  if (opts.truncate === 0) return [h('move', { attrs: { p: path } }, '[...]')];
  return ([renderMoveOf(ctx, node, opts)] as MaybeVNodes)
    .concat(comments)
    .concat(opts.inline ? renderInline(ctx, opts.inline, opts) : null)
    .concat(
      renderChildrenOf(ctx, node, {
        parentPath: path,
        isMainline: opts.isMainline,
        truncate: opts.truncate ? opts.truncate - 1 : undefined,
        withIndex: !!comments[0],
      }) || []
    );
}

function renderInline(ctx: Ctx, node: Tree.Node, opts: Opts): VNode {
  return h(
    'inline',
    renderMoveAndChildrenOf(ctx, node, {
      withIndex: true,
      parentPath: opts.parentPath,
      isMainline: false,
    })
  );
}

function renderMoveOf(ctx: Ctx, node: Tree.Node, opts: Opts): VNode {
  const path = opts.parentPath + node.id,
    content: MaybeVNodes = [
      opts.withIndex || node.ply & 1 ? moveView.renderIndex(node.ply, true) : null,
      fixCrazySan(node.san!),
    ];
  if (node.glyphs && ctx.showGlyphs) moveView.renderGlyphs(node.glyphs).forEach(g => content.push(g));
  return h(
    'move',
    {
      attrs: { p: path },
      class: nodeClasses(ctx, node, path),
    },
    content
  );
}

export default function (ctrl: AnalyseCtrl): VNode {
  const ctx: Ctx = {
    ctrl,
    truncateComments: false,
    showComputer: ctrl.showComputer() && !ctrl.retro,
    showGlyphs: !!ctrl.study || ctrl.showComputer(),
    showEval: !!ctrl.study || ctrl.showComputer(),
    currentPath: findCurrentPath(ctrl),
  };
  return h(
    'div.tview2.tview2-inline',
    {
      hook: mainHook(ctrl),
    },
    [
      ...renderInlineCommentsOf(ctx, ctrl.tree.root),
      ...(renderChildrenOf(ctx, ctrl.tree.root, {
        parentPath: '',
        isMainline: true,
      }) || []),
    ]
  );
}
