import { defined, isEmpty } from 'common/common';
import type { MaybeVNodes } from 'common/snabbdom';
import throttle from 'common/throttle';
import { notationsWithColor } from 'shogi/notation';
import { type Classes, type VNode, h } from 'snabbdom';
import { path as treePath } from 'tree';
import type { AnalyseCtrl } from '../ctrl';
import { renderInlineCommentsOf, renderMainlineCommentsOf } from './comment';

interface Ctx {
  ctrl: AnalyseCtrl;
}

interface RenderOpts {
  parentPath: string;
  isMainline: boolean;
}

const autoScroll = throttle(150, (ctrl: AnalyseCtrl, el) => {
  const cont = el.parentNode;
  const target = el.querySelector('.active');
  if (!target) {
    cont.scrollTop = ctrl.path === treePath.root ? 0 : 99999;
    return;
  }
  cont.scrollTop = target.offsetTop - cont.offsetHeight / 2 + target.offsetHeight;
});

function renderIndex(ply: number, withDots: boolean): VNode {
  return h('index', ply + (withDots ? '.' : ''));
}

function renderChildrenOf(ctx: Ctx, node: Tree.Node, opts: RenderOpts): MaybeVNodes {
  const cs = node.children;
  const main = cs[0];
  if (!main) return [];
  if (opts.isMainline) {
    const commentTags = renderMainlineCommentsOf(
      main,
      ctx.ctrl.tree.nodeAtPath(opts.parentPath),
      ctx.ctrl.data.game.variant.key,
    ).filter(c => !!c);
    if (!cs[1] && isEmpty(commentTags))
      return [
        renderIndex(main.ply, false),
        ...renderMoveAndChildrenOf(ctx, main, {
          parentPath: opts.parentPath,
          isMainline: true,
        }),
      ];
    const mainChildren = renderChildrenOf(ctx, main, {
      parentPath: opts.parentPath + main.id,
      isMainline: true,
    });
    const passOpts = {
      parentPath: opts.parentPath,
      isMainline: true,
    };
    return [
      renderIndex(main.ply, false),
      renderMoveOf(ctx, main, passOpts),
      h(
        'interrupt',
        commentTags.concat(
          renderLines(ctx, cs.slice(1), {
            parentPath: opts.parentPath,
            isMainline: true,
          }),
        ),
      ),
      ...mainChildren,
    ];
  }
  return cs[1] ? [renderLines(ctx, cs, opts)] : renderMoveAndChildrenOf(ctx, main, opts);
}

function renderLines(ctx: Ctx, nodes: Tree.Node[], opts: RenderOpts): VNode {
  return h(
    'lines',
    {
      class: { single: !!nodes[1] },
    },
    nodes.map(n =>
      h(
        'line',
        renderMoveAndChildrenOf(ctx, n, {
          parentPath: opts.parentPath,
          isMainline: false,
        }),
      ),
    ),
  );
}

function renderMoveOf(ctx: Ctx, node: Tree.Node, opts: RenderOpts): VNode {
  return opts.isMainline
    ? renderMainlineMoveOf(ctx, node, opts)
    : renderVariationMoveOf(ctx, node, opts);
}

function moveClasses(ctx: Ctx, node: Tree.Node, opts: RenderOpts): Classes {
  const path = opts.parentPath + node.id;
  const glyphIds = node.glyphs ? node.glyphs.map(g => g.id) : [];
  return {
    active: path === ctx.ctrl.path,
    inaccuracy: glyphIds.includes(6),
    mistake: glyphIds.includes(2),
    blunder: glyphIds.includes(4),
    'good-move': glyphIds.includes(1),
    brilliant: glyphIds.includes(3),
    interesting: glyphIds.includes(5),
  };
}

function renderMainlineMoveOf(ctx: Ctx, node: Tree.Node, opts: RenderOpts): VNode {
  const path = opts.parentPath + node.id;
  const classes = moveClasses(ctx, node, opts);
  return h(
    'move',
    {
      attrs: { p: path },
      class: classes,
    },
    renderMove(node),
  );
}

function renderGlyphs(glyphs: Tree.Glyph[]): VNode {
  return h(
    'span.glyphs',
    glyphs.map(glyph =>
      h(
        'glyph',
        {
          attrs: { title: glyph.name },
        },
        glyph.symbol,
      ),
    ),
  );
}

function normalizeEval(e: number): string {
  e = Math.max(Math.min(Math.round(e / 10) / 10, 99), -99);
  return (e > 0 ? '+' : '') + e.toFixed(1);
}

function renderMove(node: Tree.Node): MaybeVNodes {
  const ev = node.eval || node.ceval;
  return [
    renderNotation(node),
    renderGlyphs(node.glyphs || []),
    ev && defined(ev.cp)
      ? renderEval(normalizeEval(ev.cp))
      : ev && defined(ev.mate)
        ? renderEval(`#${ev.mate}`)
        : undefined,
  ];
}

function renderVariationMoveOf(ctx: Ctx, node: Tree.Node, opts: RenderOpts): VNode {
  const path = opts.parentPath + node.id;
  const classes = moveClasses(ctx, node, opts);
  return h(
    'move',
    {
      attrs: { p: path },
      class: classes,
    },
    [renderIndex(node.ply, true), renderNotation(node), renderGlyphs(node.glyphs || [])],
  );
}

function renderNotation(node: Tree.Node): VNode {
  const colorIcon = notationsWithColor() ? `.color-icon.${node.ply % 2 ? 'sente' : 'gote'}` : '';
  return h(`span${colorIcon}`, node.notation);
}

function renderMoveAndChildrenOf(ctx: Ctx, node: Tree.Node, opts: RenderOpts): MaybeVNodes {
  return [
    renderMoveOf(ctx, node, opts),
    ...renderInlineCommentsOf(
      node,
      ctx.ctrl.tree.nodeAtPath(opts.parentPath),
      ctx.ctrl.data.game.variant.key,
    ),
    ...renderChildrenOf(ctx, node, {
      parentPath: opts.parentPath + node.id,
      isMainline: opts.isMainline,
    }),
  ];
}

function renderEval(e: string): VNode {
  return h('eval', e);
}

function eventPath(e: Event): Tree.Path | null {
  const target = e.target as HTMLElement;
  return target.getAttribute('p') || (target.parentNode as HTMLElement).getAttribute('p');
}

export function renderMoves(ctrl: AnalyseCtrl): VNode {
  const root = ctrl.tree.root;
  const ctx = {
    ctrl: ctrl,
    showComputer: false,
  };
  const commentTags = renderMainlineCommentsOf(
    root,
    undefined,
    ctx.ctrl.data.game.variant.key,
    false,
  ).filter(c => !!c);
  return h(
    'div.analyse__tools',
    h(
      'div.analyse__moves.areplay',
      h(
        'div.tview2.tview2-column',
        {
          hook: {
            insert: vnode => {
              const el = vnode.elm as HTMLElement;
              if (ctrl.path !== treePath.root) autoScroll(ctrl, el);
              el.addEventListener('mousedown', (e: MouseEvent) => {
                if (defined(e.button) && e.button !== 0) return; // only touch or left click
                const path = eventPath(e);
                if (path) ctrl.jump(path);
                ctrl.redraw();
              });
            },
            postpatch: (_, vnode) => {
              if (ctrl.autoScrollRequested) {
                autoScroll(ctrl, vnode.elm as HTMLElement);
                ctrl.autoScrollRequested = false;
              }
            },
          },
        },
        ([isEmpty(commentTags) ? null : h('interrupt', commentTags)] as MaybeVNodes).concat(
          renderChildrenOf(ctx, root, {
            parentPath: '',
            isMainline: true,
          }),
        ),
      ),
    ),
  );
}
