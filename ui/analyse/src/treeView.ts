import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import contextMenu from './contextMenu';
import { empty, defined, dropThrottle } from 'common';
import { game } from 'game';
import { fixCrazySan } from 'chess';
import { path as treePath, ops as treeOps } from 'tree';
import * as moveView from './moveView';
import { authorText as commentAuthorText } from './study/studyComments';
import AnalyseController from './ctrl';

type Conceal = boolean | 'conceal' | 'hide' | null;
type ConcealOf = (mainline: boolean) => (path: Tree.Path, node: Tree.Node) => Conceal;

interface Ctx {
  ctrl: AnalyseController;
  conceal: boolean;
  concealOf: ConcealOf;
  showComputer: boolean;
  showGlyphs: boolean;
  showEval: boolean;
}
interface Opts {
  conceal: Conceal;
  noConceal?: boolean;
  parentPath: Tree.Path;
  isMainline: boolean;
}

const scrollThrottle = dropThrottle(200);

function autoScroll(ctrl: AnalyseController, el: HTMLElement): void {
  scrollThrottle(() => {
    const cont = el.parentNode as HTMLElement;
    if (!cont) return;
    const target = el.querySelector('.active') as HTMLElement;
    if (!target) {
      cont.scrollTop = ctrl.path === treePath.root ? 0 : 99999;
      return;
    }
    cont.scrollTop = target.offsetTop - cont.offsetHeight / 2 + target.offsetHeight;
  });
}

function pathContains(ctx: Ctx, path: Tree.Path): boolean {
  return treePath.contains(ctx.ctrl.path, path);
}

function nonEmpty(x: any): boolean {
  return !!x;
}

function renderChildrenOf(ctx: Ctx, node: Tree.Node, opts: Opts): VNode[] | undefined {
  const cs = node.children;
  const main = cs[0];
  if (!main) return;
  const conceal = opts.noConceal ? null : (opts.conceal || ctx.concealOf(true)(opts.parentPath + main.id, main));
  if (conceal === 'hide') return;
  if (opts.isMainline) {
    const isWhite = main.ply % 2 === 1,
    commentTags = renderMainlineCommentsOf(ctx, main, {
      conceal: conceal,
      withColor: true
    }).filter(nonEmpty);
    if (!cs[1] && empty(commentTags)) return [
      isWhite ? moveView.renderIndex(main.ply, false) : null,
      renderMoveAndChildrenOf(ctx, main, {
        parentPath: opts.parentPath,
        isMainline: true,
        conceal: conceal
      })
    ];
    const mainChildren = renderChildrenOf(ctx, main, {
      parentPath: opts.parentPath + main.id,
      isMainline: true,
      conceal: conceal
    });
    const passOpts = {
      parentPath: opts.parentPath,
      isMainline: true,
      conceal: conceal
    };
    return [
      isWhite ? moveView.renderIndex(main.ply, false) : null,
      renderMoveOf(ctx, main, passOpts),
      isWhite ? emptyMove(passOpts) : null,
      h('interrupt', [
        commentTags,
        renderLines(ctx, cs.slice(1), {
          parentPath: opts.parentPath,
          isMainline: true,
          conceal: conceal,
          noConceal: !conceal
        })
      ]),
      isWhite && mainChildren ? [
        moveView.renderIndex(main.ply, false),
        emptyMove(passOpts)
      ] : null,
      mainChildren
    ];
  }
  if (!cs[1]) return renderMoveAndChildrenOf(ctx, main, opts);
  return renderInlined(ctx, cs, opts) || renderLines(ctx, cs, opts);
}

function renderInlined(ctx: Ctx, nodes: Tree.Node[], opts: Opts): VNode[] | undefined {
  if (!nodes[1] || nodes[2]) return;
  var found;
  if (!treeOps.hasBranching(nodes[1], 4)) found = [0, 1];
  else if (!treeOps.hasBranching(nodes[0], 4)) found = [1, 0];
  if (found) return renderMoveAndChildrenOf(ctx, nodes[found[0]], {
    parentPath: opts.parentPath,
    isMainline: false,
    noConceal: opts.noConceal,
    inline: nodes[found[1]]
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
      withIndex: true,
      noConceal: opts.noConceal,
      truncate: n.comp && !pathContains(ctx, opts.parentPath + n.id) ? 3 : null
    }));
  }));
}

function renderMoveOf(ctx: Ctx, node: Tree.Node, opts: Opts): VNode {
  return opts.isMainline ? renderMainlineMoveOf(ctx, node, opts) : renderVariationMoveOf(ctx, node, opts);
}

function renderMainlineMoveOf(ctx: Ctx, node: Tree.Node, opts: Opts): VNode {
  const path = opts.parentPath + node.id,
  c = ctx.ctrl,
  current = (path === c.vm.initialPath && game.playable(c.data)) || (
    c.retro && c.retro.current() && c.retro.current().prev.path === path);
  return h('move', {
    attrs: { p: path },
    class: {
      active: path === c.path,
      context_menu: path === c.contextMenuPath,
      current: current,
      nongame: !current && c.gamePath && treePath.contains(path, c.gamePath) && path !== c.gamePath,
      [ctx.conceal]: opts.conceal
    }
  }, moveView.renderMove(ctx, node));
}

function renderVariationMoveOf(ctx: Ctx, node: Tree.Node, opts: Opts): VNode {
  const withIndex = opts.withIndex || node.ply % 2 === 1,
  path = opts.parentPath + node.id,
  active = path === ctx.ctrl.vm.path,
  content = [
    withIndex ? moveView.renderIndex(node.ply, true) : null,
    fixCrazySan(node.san)
  ];
  if (node.glyphs) content.push(moveView.renderGlyphs(node.glyphs));
  return h('move', {
    attrs: { p: path },
    class: {
      active: active,
      parent: !active && pathContains(ctx, path),
      context_menu: path === ctx.ctrl.contextMenuPath,
      [ctx.conceal]: opts.conceal
    }
  }, content);
}

function renderMoveAndChildrenOf(ctx: Ctx, node: Tree.Node, opts: Opts): VNode[] {
  var path = opts.parentPath + node.id;
  if (opts.truncate === 0) return [
    h('move', {
      attrs: { p: path }
    }, h('index', '[...]'))
  ];
  return [
    renderMoveOf(ctx, node, opts),
    renderVariationCommentsOf(ctx, node),
    opts.inline ? renderInline(ctx, opts.inline, opts) : null,
    renderChildrenOf(ctx, node, {
      parentPath: path,
      isMainline: opts.isMainline,
      noConceal: opts.noConceal,
      truncate: opts.truncate ? opts.truncate - 1 : null
    })
  ];
}

function renderInline(ctx: Ctx, node: Tree.Node, opts: Opts): VNode {
  return h('inline', renderMoveAndChildrenOf(ctx, node, {
    withIndex: true,
    parentPath: opts.parentPath,
    isMainline: false,
    noConceal: opts.noConceal,
    truncate: opts.truncate
  }));
}

function emptyMove(opts: Opts): VNode {
  return h('move.empty', {
    class: { [opts.conceal]: opts.conceal }
  }, '...');
}

function renderMainlineCommentsOf(ctx: Ctx, node: Tree.Node, opts: Opts): VNode[] {

  if (!ctx.ctrl.showComments || empty(node.comments)) return [];

  const colorClass = opts.withColor ? (node.ply % 2 === 0 ? 'black ' : 'white ') : '';

  return node.comments.map(comment => {
    if (comment.by === 'lichess' && !ctx.showComputer) return;
    let sel = 'comment';
    if (comment.text.indexOf('Inaccuracy.') === 0) sel += '.inaccuracy';
    else if (comment.text.indexOf('Mistake.') === 0) sel += '.mistake';
    else if (comment.text.indexOf('Blunder.') === 0) sel += '.blunder';
    if (opts.conceal) sel += '.' + opts.conceal;
    return h(sel, [
      node.comments[1] ? h('span.by', commentAuthorText(comment.by)) : null,
      truncateComment(comment.text, 400, ctx)
    ]);
  });
}

function renderVariationCommentsOf(ctx: Ctx, node: Tree.Node): VNode[] {
  if (!ctx.ctrl.showComments || empty(node.comments)) return [];
  return node.comments.map(comment => {
    if (comment.by === 'lichess' && !ctx.showComputer) return;
    return h('comment', [
      node.comments[1] ? h('span.by', commentAuthorText(comment.by)) : null,
      truncateComment(comment.text, 300, ctx)
    ]);
  });
}

function truncateComment(text: string, len: number, ctx: Ctx) {
  return ctx.ctrl.embed || text.length <= len ? text : text.slice(0, len - 10) + ' [...]';
}

function eventPath(e: MouseEvent): Tree.Path | undefined {
  return e.target.getAttribute('p') || e.target.parentNode.getAttribute('p');
}

function noop() {}

function emptyConcealOf() {
  return noop;
}

export default function(ctrl: AnalyseController, concealOf: ConcealOf): VNode {
  const root = ctrl.tree.root;
  const ctx: Ctx = {
    ctrl: ctrl,
    concealOf: concealOf || emptyConcealOf,
    showComputer: ctrl.vm.showComputer() && !ctrl.retro,
    showGlyphs: !!ctrl.study || ctrl.vm.showComputer(),
    showEval: !!ctrl.study || ctrl.vm.showComputer()
  };
  const commentTags = renderMainlineCommentsOf(ctx, root, {
    withColor: false,
    conceal: false
  });
  return h('div.tview2', {
    config: function(el, isUpdate) {
      if (ctrl.vm.autoScrollRequested || !isUpdate) {
        if (isUpdate || ctrl.vm.path !== treePath.root) autoScroll(ctrl, el);
        ctrl.vm.autoScrollRequested = false;
      }
      if (isUpdate) return;
      el.oncontextmenu = function(e) {
        var path = eventPath(e, ctrl);
        if (path !== null) contextMenu.open(e, {
          path: path,
          root: ctrl
        });
        m.redraw();
        return false;
      };
      el.addEventListener('mousedown', function(e) {
        if (defined(e.button) && e.button !== 0) return; // only touch or left click
        var path = eventPath(e, ctrl);
        if (path) ctrl.userJump(path);
        m.redraw();
      });
    },
  }, [
    empty(commentTags) ? null : m('interrupt', commentTags),
    root.ply % 2 === 1 ? [
      moveView.renderIndex(root.ply, false),
      emptyMove({})
    ] : null,
    renderChildrenOf(ctx, root, {
      parentPath: '',
      isMainline: true
    })
  ]);
}
};
