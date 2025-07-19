import { h, type VNode } from 'snabbdom';
import { fixCrazySan } from 'lib/game/chess';
import { path as treePath, ops as treeOps } from 'lib/tree/tree';
import * as moveView from '../view/moveView';
import type AnalyseCtrl from '../ctrl';
import * as licon from 'lib/licon';
import type { MaybeVNodes } from 'lib/snabbdom';
import { mainHook, nodeClasses, renderInlineCommentsOf, retroLine, Ctx, Opts, renderingCtx } from './common';

function renderChildrenOf(ctx: Ctx, node: Tree.Node, opts: Opts): MaybeVNodes | undefined {
  const cs = node.children.filter(x => ctx.showComputer || !x.comp),
    main = cs[0];
  if (!main) return;
  if (opts.isMainline) {
    if (!cs[1] && !main.forceVariation)
      return renderMoveAndChildrenOf(ctx, main, {
        parentPath: opts.parentPath,
        isMainline: true,
        depth: opts.depth,
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
                depth: opts.depth,
                withIndex: opts.withIndex,
              }),
              ...renderInlineCommentsOf(ctx, main, opts.parentPath),
            ]),
        h(
          'interrupt',
          renderLines(ctx, node, main.forceVariation ? cs : cs.slice(1), {
            parentPath: opts.parentPath,
            isMainline: true,
            depth: opts.depth,
          }),
        ),
        ...(main.forceVariation
          ? []
          : renderChildrenOf(ctx, main, {
              parentPath: opts.parentPath + main.id,
              isMainline: true,
              depth: opts.depth,
              withIndex: true,
            }) || []),
      ]
    );
  }
  if (!cs[1]) return renderMoveAndChildrenOf(ctx, main, opts);
  return renderInlined(ctx, cs, opts) || [renderLines(ctx, node, cs, opts)];
}

function renderInlined(ctx: Ctx, nodes: Tree.Node[], opts: Opts): MaybeVNodes | undefined {
  // only 2 branches
  if (!nodes[1] || nodes[2] || nodes[0].forceVariation) return;
  // only if second branch has no sub-branches
  if (treeOps.hasBranching(nodes[1], 6)) return;
  return renderMoveAndChildrenOf(ctx, nodes[0], {
    parentPath: opts.parentPath,
    isMainline: opts.isMainline,
    depth: opts.depth,
    inline: nodes[1],
  });
}

function renderLines(ctx: Ctx, parentNode: Tree.Node, nodes: Tree.Node[], opts: Opts): VNode {
  const collapsed =
    parentNode.collapsed === undefined ? opts.depth >= 2 && opts.depth % 2 === 0 : parentNode.collapsed;
  return h(
    'lines',
    { class: { collapsed } },
    collapsed
      ? h('line', { class: { expand: true } }, [
          h('branch'),
          h('a', {
            attrs: { 'data-icon': licon.PlusButton, title: i18n.site.expandVariations },
            on: { click: () => ctx.ctrl.setCollapsed(opts.parentPath, false) },
          }),
        ])
      : nodes.map(n => {
          return (
            retroLine(ctx, n) ||
            h('line', [
              h('branch'),
              ...renderMoveAndChildrenOf(ctx, n, {
                parentPath: opts.parentPath,
                isMainline: false,
                depth: opts.depth + 1,
                withIndex: true,
                truncate: n.comp && !treePath.contains(ctx.ctrl.path, opts.parentPath + n.id) ? 3 : undefined,
              }),
            ])
          );
        }),
  );
}

function renderMoveAndChildrenOf(ctx: Ctx, node: Tree.Node, opts: Opts): MaybeVNodes {
  const path = opts.parentPath + node.id,
    comments = renderInlineCommentsOf(ctx, node, path);
  if (opts.truncate === 0) return [h('move', { attrs: { p: path } }, '[...]')];
  return ([renderMoveOf(ctx, node, opts)] as MaybeVNodes)
    .concat(comments)
    .concat(opts.inline ? renderInline(ctx, opts.inline, opts) : null)
    .concat(
      renderChildrenOf(ctx, node, {
        parentPath: path,
        isMainline: opts.isMainline,
        depth: opts.depth,
        truncate: !!opts.truncate ? opts.truncate - 1 : undefined,
        withIndex: !!comments[0],
      }) || [],
    );
}

function renderInline(ctx: Ctx, node: Tree.Node, opts: Opts): VNode {
  const retro = retroLine(ctx, node);
  if (retro) return h('interrupt', h('lines', retro));
  return h(
    'inline',
    renderMoveAndChildrenOf(ctx, node, {
      withIndex: true,
      parentPath: opts.parentPath,
      isMainline: false,
      depth: opts.depth,
    }),
  );
}

function renderMoveOf(ctx: Ctx, node: Tree.Node, opts: Opts): VNode {
  const path = opts.parentPath + node.id,
    content: MaybeVNodes = [
      opts.withIndex || node.ply & 1 ? moveView.renderIndex(node.ply, true) : null,
      fixCrazySan(node.san!),
    ];
  if (node.glyphs && ctx.showGlyphs) node.glyphs.forEach(g => content.push(moveView.renderGlyph(g)));
  return h('move', { attrs: { p: path }, class: nodeClasses(ctx, node, path) }, content);
}

export default function (ctrl: AnalyseCtrl): VNode {
  const ctx = renderingCtx(ctrl);
  return h('div.tview2.tview2-inline', { hook: mainHook(ctrl) }, [
    ...renderInlineCommentsOf(ctx, ctrl.tree.root, ''),
    ...(renderChildrenOf(ctx, ctrl.tree.root, {
      parentPath: '',
      isMainline: true,
      depth: 0,
    }) || []),
  ]);
}
