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
import { autoScroll, renderMainlineCommentsOf, mainHook } from './treeView';
import { Ctx, Opts } from './treeView';

function renderChildrenOf(ctx: Ctx, node: Tree.Node, opts: Opts): MaybeVNodes | undefined {
  return [h('div', 'children')];
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
  return h('div.tview2', {
    hook: mainHook(ctrl)
  }, ([
    empty(commentTags) ? null : h('interrupt', commentTags),
    root.ply & 1 ? moveView.renderIndex(root.ply, false) : null,
    root.ply & 1 ? emptyMove() : null
  ] as MaybeVNodes).concat(renderChildrenOf(ctx, root, {
    parentPath: '',
    isMainline: true
  }) || []));
}
