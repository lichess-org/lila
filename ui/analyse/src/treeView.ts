import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import contextMenu from './contextMenu';
import { empty, defined, dropThrottle } from 'common';
import { game } from 'game';
import { fixCrazySan } from 'chess';
import { path as treePath, ops as treeOps } from 'tree';
import * as moveView from './moveView';
import { authorText as commentAuthorText } from './study/studyComments';
import AnalyseCtrl from './ctrl';
import { MaybeVNodes, ConcealOf, Conceal } from './interfaces';

interface Ctx {
  ctrl: AnalyseCtrl;
  concealOf: ConcealOf;
  showComputer: boolean;
  showGlyphs: boolean;
  showEval: boolean;
}
interface Opts {
  conceal?: Conceal;
  noConceal?: boolean;
  parentPath: Tree.Path;
  isMainline: boolean;
  inline?: Tree.Node;
  withIndex?: boolean;
  truncate?: number;
}

const scrollThrottle = dropThrottle(200);

function autoScroll(ctrl: AnalyseCtrl, el: HTMLElement): void {
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

function renderChildrenOf(ctx: Ctx, node: Tree.Node, opts: Opts): MaybeVNodes | undefined {
  const cs = node.children;
  const main = cs[0];
  if (!main) return;
  const conceal = opts.noConceal ? null : (opts.conceal || ctx.concealOf(true)(opts.parentPath + main.id, main));
  if (conceal === 'hide') return;
  if (opts.isMainline) {
    const isWhite = main.ply % 2 === 1,
    commentTags = renderMainlineCommentsOf(ctx, main, conceal, true).filter(nonEmpty);
    if (!cs[1] && empty(commentTags)) return ((isWhite ? [moveView.renderIndex(main.ply, false)] : []) as MaybeVNodes).concat(
      renderMoveAndChildrenOf(ctx, main, {
        parentPath: opts.parentPath,
        isMainline: true,
        conceal
      }) || []
    );
    const mainChildren = renderChildrenOf(ctx, main, {
      parentPath: opts.parentPath + main.id,
      isMainline: true,
      conceal
    });
    const passOpts = {
      parentPath: opts.parentPath,
      isMainline: true,
      conceal
    };
    return (isWhite ? [moveView.renderIndex(main.ply, false)] : [] as MaybeVNodes).concat([
      renderMoveOf(ctx, main, passOpts),
      isWhite ? emptyMove(passOpts.conceal) : null,
      h('interrupt', commentTags.concat(
        renderLines(ctx, cs.slice(1), {
          parentPath: opts.parentPath,
          isMainline: true,
          conceal,
          noConceal: !conceal
        })
      ))
    ] as MaybeVNodes).concat(
      isWhite && mainChildren ? [
        moveView.renderIndex(main.ply, false),
        emptyMove(passOpts.conceal)
      ] : []).concat(mainChildren || []);
  }
  if (!cs[1]) return renderMoveAndChildrenOf(ctx, main, opts);
  return renderInlined(ctx, cs, opts) || [renderLines(ctx, cs, opts)];
}

function renderInlined(ctx: Ctx, nodes: Tree.Node[], opts: Opts): MaybeVNodes | undefined {
  // only 2 branches
  if (!nodes[1] || nodes[2]) return;
  // only if second branch has no sub-branches
  if (treeOps.hasBranching(nodes[1], 4)) return;
  return renderMoveAndChildrenOf(ctx, nodes[0], {
    parentPath: opts.parentPath,
    isMainline: false,
    noConceal: opts.noConceal,
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
      withIndex: true,
      noConceal: opts.noConceal,
      truncate: n.comp && !pathContains(ctx, opts.parentPath + n.id) ? 3 : undefined
    }));
  }));
}

function renderMoveOf(ctx: Ctx, node: Tree.Node, opts: Opts): VNode {
  return opts.isMainline ? renderMainlineMoveOf(ctx, node, opts) : renderVariationMoveOf(ctx, node, opts);
}

function renderMainlineMoveOf(ctx: Ctx, node: Tree.Node, opts: Opts): VNode {
  const path = opts.parentPath + node.id,
  c = ctx.ctrl,
  current = (path === c.initialPath && game.playable(c.data)) || (
    c.retro && c.retro.current() && c.retro.current().prev.path === path),
  classes = {
    active: path === c.path,
    context_menu: path === c.contextMenuPath,
    current,
    nongame: !current && !!c.gamePath && treePath.contains(path, c.gamePath) && path !== c.gamePath
  };
  if (opts.conceal) classes[opts.conceal as string] = true;
  return h('move', {
    attrs: { p: path },
    class: classes
  }, moveView.renderMove(ctx, node));
}

function renderVariationMoveOf(ctx: Ctx, node: Tree.Node, opts: Opts): VNode {
  const withIndex = opts.withIndex || node.ply % 2 === 1,
  path = opts.parentPath + node.id,
  active = path === ctx.ctrl.path,
  content: MaybeVNodes = [
    withIndex ? moveView.renderIndex(node.ply, true) : null,
    fixCrazySan(node.san!)
  ],
  classes = {
    active,
    parent: !active && pathContains(ctx, path),
    context_menu: path === ctx.ctrl.contextMenuPath,
  };
  if (opts.conceal) classes[opts.conceal as string] = true;
  if (node.glyphs) moveView.renderGlyphs(node.glyphs).forEach(g => content.push(g));
  return h('move', {
    attrs: { p: path },
    class: classes
  }, content);
}

function renderMoveAndChildrenOf(ctx: Ctx, node: Tree.Node, opts: Opts): MaybeVNodes {
  var path = opts.parentPath + node.id;
  if (opts.truncate === 0) return [
    h('move', {
      attrs: { p: path }
    }, [h('index', '[...]')])
  ];
  return ([renderMoveOf(ctx, node, opts)] as MaybeVNodes)
    .concat(renderVariationCommentsOf(ctx, node))
    .concat(opts.inline ? renderInline(ctx, opts.inline, opts) : null)
    .concat(renderChildrenOf(ctx, node, {
      parentPath: path,
      isMainline: opts.isMainline,
      noConceal: opts.noConceal,
      truncate: opts.truncate ? opts.truncate - 1 : undefined
    }) || []);
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

function emptyMove(conceal?: Conceal): VNode {
  const c = {};
  if (conceal) c[conceal as string] = true;
  return h('move.empty', {
    class: c
  }, '...');
}

function renderMainlineCommentsOf(ctx: Ctx, node: Tree.Node, conceal: Conceal, withColor: boolean): MaybeVNodes {

  if (!ctx.ctrl.showComments || empty(node.comments)) return [];

  const colorClass = withColor ? (node.ply % 2 === 0 ? '.black ' : '.white ') : '';

  return node.comments!.map(comment => {
    if (comment.by === 'lichess' && !ctx.showComputer) return;
    let sel = 'comment' + colorClass;
    if (comment.text.indexOf('Inaccuracy.') === 0) sel += '.inaccuracy';
    else if (comment.text.indexOf('Mistake.') === 0) sel += '.mistake';
    else if (comment.text.indexOf('Blunder.') === 0) sel += '.blunder';
    if (conceal) sel += '.' + conceal;
    return h(sel, [
      node.comments![1] ? h('span.by', commentAuthorText(comment.by)) : null,
      truncateComment(comment.text, 400, ctx)
    ]);
  });
}

function renderVariationCommentsOf(ctx: Ctx, node: Tree.Node): MaybeVNodes {
  if (!ctx.ctrl.showComments || empty(node.comments)) return [];
  return node.comments!.map(comment => {
    if (comment.by === 'lichess' && !ctx.showComputer) return;
    return h('comment', [
      node.comments![1] ? h('span.by', commentAuthorText(comment.by)) : null,
      truncateComment(comment.text, 300, ctx)
    ]);
  });
}

function truncateComment(text: string, len: number, ctx: Ctx) {
  return ctx.ctrl.embed || text.length <= len ? text : text.slice(0, len - 10) + ' [...]';
}

function eventPath(e: MouseEvent): Tree.Path | null {
  return (e.target as HTMLElement).getAttribute('p') ||
  ((e.target as HTMLElement).parentNode as HTMLElement).getAttribute('p');
}

const emptyConcealOf: ConcealOf = function() {
  return function() { return null; };
};

export default function(ctrl: AnalyseCtrl, concealOf?: ConcealOf): VNode {
  const root = ctrl.tree.root;
  const ctx: Ctx = {
    ctrl,
    concealOf: concealOf || emptyConcealOf,
    showComputer: ctrl.showComputer() && !ctrl.retro,
    showGlyphs: !!ctrl.study || ctrl.showComputer(),
    showEval: !!ctrl.study || ctrl.showComputer()
  };
  const commentTags = renderMainlineCommentsOf(ctx, root, false, false);
  return h('div.tview2', {
    hook: {
      insert: vnode => {
        const el = vnode.elm as HTMLElement;
        if (ctrl.path !== treePath.root) autoScroll(ctrl, el);
        el.oncontextmenu = (e: MouseEvent) => {
          const path = eventPath(e);
          if (path !== null) contextMenu(e, {
            path,
            root: ctrl
          });
          ctrl.redraw();
          return false;
        };
        el.addEventListener('mousedown', (e: MouseEvent) => {
          if (defined(e.button) && e.button !== 0) return; // only touch or left click
          const path = eventPath(e);
          if (path) ctrl.userJump(path);
          ctrl.redraw();
        });
      },
      postpatch: (_, vnode) => {
        if (ctrl.autoScrollRequested && ctrl.path !== treePath.root) {
          autoScroll(ctrl, vnode.elm as HTMLElement);
          ctrl.autoScrollRequested = false;
        }
      }
    }
  }, ([
    empty(commentTags) ? null : h('interrupt', commentTags),
    root.ply & 1 ? moveView.renderIndex(root.ply, false) : null,
    root.ply & 1 ? emptyMove() : null
  ] as MaybeVNodes).concat(renderChildrenOf(ctx, root, {
    parentPath: '',
    isMainline: true
  }) || []));
}
