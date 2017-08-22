import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
// import contextMenu from '../contextMenu';
import { empty, defined } from 'common';
// import { game } from 'game';
// import { fixCrazySan } from 'chess';
// import { path as treePath, ops as treeOps } from 'tree';
import * as moveView from '../moveView';
// import { authorText as commentAuthorText } from '../study/studyComments';
import AnalyseCtrl from '../ctrl';
import { MaybeVNodes } from '../interfaces';
import { autoScroll, renderMainlineCommentsOf, mainHook, nodeClasses } from './treeView';
import { Ctx, Opts } from './treeView';

function renderNode(ctx: Ctx, node: Tree.Node, opts: Opts): MaybeVNodes {
  const path = opts.parentPath + node.id,
  c = ctx.ctrl;
  return [
    h('move', {
      attrs: { p: path },
      class: nodeClasses(c, path)
    }, [
      node.ply & 1 ? moveView.renderIndexText(node.ply, false) + ' ' : null,
      ...moveView.renderMove(ctx, node)
    ]),
    ...(renderChildrenOf(ctx, node, {
      parentPath: path,
      isMainline: true
    }) || [])
  ];
}

function renderChildrenOf(ctx: Ctx, node: Tree.Node, opts: Opts): MaybeVNodes | undefined {
  const cs = node.children;
  const main = cs[0];
  if (!main) return;
  return renderNode(ctx, main, {
    parentPath: opts.parentPath,
    isMainline: true
  });
}

function emptyMove(): VNode {
  return h('move.empty', '...');
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
