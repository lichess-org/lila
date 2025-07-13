import type { VNode, Classes } from 'snabbdom';
import { defined } from 'lib';
import { throttle } from 'lib/async';
import { renderEval as normalizeEval } from 'lib/ceval/ceval';
import { path as treePath } from 'lib/tree/tree';
import { type MaybeVNode, type LooseVNodes, hl } from 'lib/snabbdom';
import type PuzzleCtrl from '../ctrl';
import { plyToTurn } from 'lib/game/chess';

interface Ctx {
  ctrl: PuzzleCtrl;
}

interface RenderOpts {
  parentPath: string;
  isMainline: boolean;
  withIndex?: boolean;
}

interface Glyph {
  name: string;
  symbol: string;
}

const autoScroll = throttle(150, (ctrl: PuzzleCtrl, el: HTMLElement) => {
  const cont = el.parentNode as HTMLElement;
  const target = el.querySelector('.active') as HTMLElement | null;
  if (!target) {
    cont.scrollTop = ctrl.path === treePath.root ? 0 : 99999;
    return;
  }
  const targetOffset = target.getBoundingClientRect().y - el.getBoundingClientRect().y;
  cont.scrollTop = targetOffset - cont.offsetHeight / 2 + target.offsetHeight;
});

function pathContains(ctx: Ctx, path: Tree.Path): boolean {
  return treePath.contains(ctx.ctrl.path, path);
}

export function renderIndex(ply: number, withDots: boolean): VNode {
  return hl('index', plyToTurn(ply) + (withDots ? (ply % 2 === 1 ? '.' : '...') : ''));
}

function renderChildrenOf(ctx: Ctx, node: Tree.Node, opts: RenderOpts): LooseVNodes {
  const cs = node.children,
    main = cs[0];
  if (!main) return [];
  if (opts.isMainline) {
    const isWhite = main.ply % 2 === 1;
    if (!cs[1])
      return [
        isWhite && renderIndex(main.ply, false),
        renderMoveAndChildrenOf(ctx, main, { parentPath: opts.parentPath, isMainline: true }),
      ];
    const mainChildren = renderChildrenOf(ctx, main, {
        parentPath: opts.parentPath + main.id,
        isMainline: true,
      }),
      passOpts = { parentPath: opts.parentPath, isMainline: true };
    return [
      isWhite && renderIndex(main.ply, false),
      renderMoveOf(ctx, main, passOpts),
      isWhite && emptyMove(),
      hl('interrupt', renderLines(ctx, cs.slice(1), { parentPath: opts.parentPath, isMainline: true })),
      isWhite && mainChildren && [renderIndex(main.ply, false), emptyMove()],
      mainChildren,
    ];
  }
  return cs[1] ? [renderLines(ctx, cs, opts)] : renderMoveAndChildrenOf(ctx, main, opts);
}

function renderLines(ctx: Ctx, nodes: Tree.Node[], opts: RenderOpts): VNode {
  return hl(
    'lines',
    { class: { single: !!nodes[1] } },
    nodes.map(function (n) {
      return hl(
        'line',
        renderMoveAndChildrenOf(ctx, n, { parentPath: opts.parentPath, isMainline: false, withIndex: true }),
      );
    }),
  );
}

function renderMoveOf(ctx: Ctx, node: Tree.Node, opts: RenderOpts): VNode {
  return opts.isMainline ? renderMainlineMoveOf(ctx, node, opts) : renderVariationMoveOf(ctx, node, opts);
}

function renderMainlineMoveOf(ctx: Ctx, node: Tree.Node, opts: RenderOpts): VNode {
  const path = opts.parentPath + node.id;
  const classes: Classes = {
    active: path === ctx.ctrl.path,
    current: path === ctx.ctrl.initialPath,
    hist: node.ply < ctx.ctrl.initialNode.ply,
  };
  if (node.puzzle) classes[node.puzzle] = true;
  return hl('move', { attrs: { p: path }, class: classes }, renderMove(node));
}

const renderGlyph = (glyph: Glyph): VNode => hl('glyph', { attrs: { title: glyph.name } }, glyph.symbol);

function puzzleGlyph(node: Tree.Node): MaybeVNode {
  switch (node.puzzle) {
    case 'good':
    case 'win':
      return renderGlyph({ name: i18n.puzzle.bestMove, symbol: '✓' });
    case 'fail':
      return renderGlyph({
        name: 'Puzzle failed', //puzzleFailed key never worked, it's in learn/*.xml
        symbol: '✗',
      });
    case 'retry':
      return renderGlyph({ name: i18n.puzzle.goodMove, symbol: '?!' });
    default:
      return;
  }
}

function renderMove(node: Tree.Node): LooseVNodes {
  const ev = node.eval || node.ceval;
  return [
    node.san,
    ev && (defined(ev.cp) ? renderEval(normalizeEval(ev.cp)) : defined(ev.mate) && renderEval('#' + ev.mate)),
    puzzleGlyph(node),
  ];
}

function renderVariationMoveOf(ctx: Ctx, node: Tree.Node, opts: RenderOpts): VNode {
  const withIndex = opts.withIndex || node.ply % 2 === 1;
  const path = opts.parentPath + node.id;
  const active = path === ctx.ctrl.path;
  const classes: Classes = { active, parent: !active && pathContains(ctx, path) };
  if (node.puzzle) classes[node.puzzle] = true;
  return hl('move', { attrs: { p: path }, class: classes }, [
    withIndex && renderIndex(node.ply, true),
    node.san,
    puzzleGlyph(node),
  ]);
}

function renderMoveAndChildrenOf(ctx: Ctx, node: Tree.Node, opts: RenderOpts): LooseVNodes {
  return [
    renderMoveOf(ctx, node, opts),
    renderChildrenOf(ctx, node, { parentPath: opts.parentPath + node.id, isMainline: opts.isMainline }),
  ];
}

function emptyMove(): VNode {
  return hl('move.empty', '...');
}

function renderEval(e: string): VNode {
  return hl('eval', e);
}

function eventPath(e: Event): Tree.Path | null {
  const target = e.target as HTMLElement;
  return target.getAttribute('p') || (target.parentNode as HTMLElement).getAttribute('p');
}

export function render(ctrl: PuzzleCtrl): VNode {
  const root = ctrl.tree.root;
  const ctx = { ctrl: ctrl, showComputer: false };
  return hl(
    'div.tview2.tview2-column',
    {
      hook: {
        insert: vnode => {
          const el = vnode.elm as HTMLElement;
          if (ctrl.path !== treePath.root) autoScroll(ctrl, el);
          el.addEventListener('mousedown', (e: MouseEvent) => {
            if (defined(e.button) && e.button !== 0) return; // only touch or left click
            const path = eventPath(e);
            if (path) ctrl.userJump(path);
            ctrl.redraw();
          });
        },
        postpatch: (_, vnode) => {
          if (ctrl.autoScrollNow) {
            autoScroll(ctrl, vnode.elm as HTMLElement);
            ctrl.autoScrollNow = false;
            ctrl.autoScrollRequested = false;
          } else if (ctrl.autoScrollRequested) {
            if (ctrl.path !== treePath.root) autoScroll(ctrl, vnode.elm as HTMLElement);
            ctrl.autoScrollRequested = false;
          }
        },
      },
    },
    [
      root.ply % 2 === 1 && [renderIndex(root.ply, false), emptyMove()],
      renderChildrenOf(ctx, root, { parentPath: '', isMainline: true }),
    ],
  );
}
