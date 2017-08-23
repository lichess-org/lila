import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
// import contextMenu from '../contextMenu';
import { empty, defined } from 'common';
// import { game } from 'game';
import { fixCrazySan } from 'chess';
import { path as treePath, ops as treeOps } from 'tree';
import * as moveView from '../moveView';
// import { authorText as commentAuthorText } from '../study/studyComments';
import AnalyseCtrl from '../ctrl';
import { MaybeVNodes } from '../interfaces';
import { autoScroll, renderMainlineCommentsOf, mainHook, nodeClasses } from './treeView';
import { Ctx, Opts } from './treeView';

function renderChildrenOf(ctx: Ctx, node: Tree.Node, opts: Opts): MaybeVNodes | undefined {
  const cs = node.children,
  main = cs[0];
  if (!main) return;
  if (opts.isMainline) {
    const isWhite = main.ply % 2 === 1,
    commentTags: MaybeVNodes = []; //renderMainlineCommentsOf(ctx, main, conceal, true).filter(nonEmpty);
    if (!cs[1] && empty(commentTags)) return renderMoveAndChildrenOf(ctx, main, {
      parentPath: opts.parentPath,
      isMainline: true,
    });
    const mainChildren = renderChildrenOf(ctx, main, {
      parentPath: opts.parentPath + main.id,
      isMainline: true,
    });
    const passOpts = {
      parentPath: opts.parentPath,
      isMainline: true,
    };
    return (isWhite ? [moveView.renderIndex(main.ply, false)] : [] as MaybeVNodes).concat([
      renderMoveOf(ctx, main, passOpts),
      h('interrupt', commentTags.concat(
        renderLines(ctx, cs.slice(1), {
          parentPath: opts.parentPath,
          isMainline: true
        })
      ))
    ] as MaybeVNodes).concat(
      isWhite && mainChildren ? [
        moveView.renderIndex(main.ply, false),
      ] : []).concat(mainChildren || []);
  }
  if (!cs[1]) return renderMoveAndChildrenOf(ctx, main, opts);
  return renderInlined(ctx, cs, opts) || [renderLines(ctx, cs, opts)];
  // return renderNode(ctx, main, {
  //   parentPath: opts.parentPath,
  //   isMainline: true
  // });
  }

function renderInlined(ctx: Ctx, nodes: Tree.Node[], opts: Opts): MaybeVNodes | undefined {
  // only 2 branches
  if (!nodes[1] || nodes[2]) return;
  // only if second branch has no sub-branches
  if (treeOps.hasBranching(nodes[1], 4)) return;
  return renderMoveAndChildrenOf(ctx, nodes[0], {
    parentPath: opts.parentPath,
    isMainline: opts.isMainline,
    inline: nodes[1]
  });
}

function renderLines(ctx: Ctx, nodes: Tree.Node[], opts: Opts): VNode {
  return h('lines', {
    class: { single: !nodes[1] }
  }, nodes.map(n => {
    if (n.comp && ctx.ctrl.retro && ctx.ctrl.retro.hideComputerLine(n, opts.parentPath))
    return h('line', 'Learn from this mistake');
    return h('line', renderMoveAndChildrenOf(ctx, n, {
      parentPath: opts.parentPath,
      isMainline: false,
      withIndex: true
    }));
  }));
}

function renderMoveAndChildrenOf(ctx: Ctx, node: Tree.Node, opts: Opts): MaybeVNodes {
  const path = opts.parentPath + node.id;
  if (opts.truncate === 0) return [
    h('move', {
      attrs: { p: path }
    }, [h('index', '[...]')])
  ];
  return ([renderMoveOf(ctx, node, opts)] as MaybeVNodes)
  // .concat(renderVariationCommentsOf(ctx, node))
  .concat(opts.inline ? renderInline(ctx, opts.inline, opts) : null)
  .concat(renderChildrenOf(ctx, node, {
    parentPath: path,
    isMainline: opts.isMainline,
    truncate: opts.truncate ? opts.truncate - 1 : undefined
  }) || []);
}

function renderInline(ctx: Ctx, node: Tree.Node, opts: Opts): VNode {
  return h('inline', renderMoveAndChildrenOf(ctx, node, {
    withIndex: true,
    parentPath: opts.parentPath,
    isMainline: false
  }));
}

function renderMoveOf(ctx: Ctx, node: Tree.Node, opts: Opts): VNode {
  const withIndex = opts.withIndex || node.ply % 2 === 1,
  path = opts.parentPath + node.id,
  content: MaybeVNodes = [
    withIndex ? moveView.renderIndex(node.ply, true) : null,
    fixCrazySan(node.san!)
  ];
  if (node.glyphs) moveView.renderGlyphs(node.glyphs).forEach(g => content.push(g));
  return h('move', {
    attrs: { p: path },
    class: nodeClasses(ctx.ctrl, path)
  }, content);
}

export default function(ctrl: AnalyseCtrl): VNode {
  const root = ctrl.tree.root;
  const ctx: Ctx = {
    ctrl,
    truncateComments: false,
    showComputer: ctrl.showComputer() && !ctrl.retro,
    showGlyphs: !!ctrl.study || ctrl.showComputer(),
    showEval: !!ctrl.study || ctrl.showComputer()
  };
  const commentTags = renderMainlineCommentsOf(ctx, root, false, false);
  return h('div.tview2.literal', {
    hook: mainHook(ctrl)
  }, [
    empty(commentTags) ? null : h('interrupt', commentTags),
    ...(renderChildrenOf(ctx, root, {
      parentPath: '',
      isMainline: true
    }) || [])
  ]);
}
