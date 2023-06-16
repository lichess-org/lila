import { renderEval as normalizeEval } from 'ceval';
import { defined } from 'common/common';
import { notationsWithColor } from 'common/notation';
import { MaybeVNode, MaybeVNodes } from 'common/snabbdom';
import throttle from 'common/throttle';
import { Classes, VNode, h } from 'snabbdom';
import { path as treePath } from 'tree';
import { Controller } from '../interfaces';

interface Ctx {
  ctrl: Controller;
}

interface RenderOpts {
  parentPath: string;
  isMainline: boolean;
}

interface Glyph {
  name: string;
  symbol: string;
}

const autoScroll = throttle(150, (ctrl: Controller, el) => {
  var cont = el.parentNode;
  var target = el.querySelector('.active');
  if (!target) {
    cont.scrollTop = ctrl.vm.path === treePath.root ? 0 : 99999;
    return;
  }
  cont.scrollTop = target.offsetTop - cont.offsetHeight / 2 + target.offsetHeight;
});

export function renderIndex(ply: number, withDots: boolean): VNode {
  return h('index', ply + (withDots ? '.' : ''));
}

function renderChildrenOf(ctx: Ctx, node: Tree.Node, opts: RenderOpts): MaybeVNodes {
  const cs = node.children,
    main = cs[0];
  if (!main) return [];
  if (opts.isMainline) {
    if (!cs[1])
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
      }),
      passOpts = {
        parentPath: opts.parentPath,
        isMainline: true,
      };
    return [
      renderIndex(main.ply, false),
      renderMoveOf(ctx, main, passOpts),
      h(
        'interrupt',
        renderLines(ctx, cs.slice(1), {
          parentPath: opts.parentPath,
          isMainline: true,
        })
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
    nodes.map(function (n) {
      return h(
        'line',
        renderMoveAndChildrenOf(ctx, n, {
          parentPath: opts.parentPath,
          isMainline: false,
        })
      );
    })
  );
}

function renderMoveOf(ctx: Ctx, node: Tree.Node, opts: RenderOpts): VNode {
  return opts.isMainline ? renderMainlineMoveOf(ctx, node, opts) : renderVariationMoveOf(ctx, node, opts);
}

function renderMainlineMoveOf(ctx: Ctx, node: Tree.Node, opts: RenderOpts): VNode {
  const path = opts.parentPath + node.id;
  const classes: Classes = {
    active: path === ctx.ctrl.vm.path,
    current: path === ctx.ctrl.vm.initialPath,
    hist: node.ply < ctx.ctrl.vm.initialNode.ply,
  };
  if (node.puzzle) classes[node.puzzle] = true;
  return h(
    'move',
    {
      attrs: { p: path },
      class: classes,
    },
    renderMove(ctx, node)
  );
}

function renderGlyph(glyph: Glyph): VNode {
  return h(
    'glyph',
    {
      attrs: { title: glyph.name },
    },
    glyph.symbol
  );
}

function puzzleGlyph(ctx: Ctx, node: Tree.Node): MaybeVNode {
  switch (node.puzzle) {
    case 'good':
    case 'win':
      return renderGlyph({
        name: ctx.ctrl.trans.noarg('bestMove'),
        symbol: '✓',
      });
    case 'fail':
      return renderGlyph({
        name: ctx.ctrl.trans.noarg('mistake'),
        symbol: '✗',
      });
    default:
      return;
  }
}

export function renderMove(ctx: Ctx, node: Tree.Node): MaybeVNodes {
  const ev = node.eval || node.ceval;
  return [
    renderNotation(node),
    puzzleGlyph(ctx, node),
    ev &&
      (defined(ev.cp) ? renderEval(normalizeEval(ev.cp)) : defined(ev.mate) ? renderEval('#' + ev.mate) : undefined),
  ];
}

function renderVariationMoveOf(ctx: Ctx, node: Tree.Node, opts: RenderOpts): VNode {
  const path = opts.parentPath + node.id;
  const active = path === ctx.ctrl.vm.path;
  const classes: Classes = {
    active,
  };
  if (node.puzzle) classes[node.puzzle] = true;
  return h(
    'move',
    {
      attrs: { p: path },
      class: classes,
    },
    [renderIndex(node.ply, true), renderNotation(node), puzzleGlyph(ctx, node)]
  );
}

function renderNotation(node: Tree.Node): VNode {
  const colorIcon = notationsWithColor() ? '.color-icon.' + (node.ply % 2 ? 'sente' : 'gote') : '';
  return h('span' + colorIcon, node.notation);
}

function renderMoveAndChildrenOf(ctx: Ctx, node: Tree.Node, opts: RenderOpts): MaybeVNodes {
  return [
    renderMoveOf(ctx, node, opts),
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

export function render(ctrl: Controller): VNode {
  const root = ctrl.getTree().root;
  const ctx = {
    ctrl: ctrl,
    showComputer: false,
  };
  return h(
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
          if (ctrl.vm.autoScrollNow) {
            autoScroll(ctrl, vnode.elm as HTMLElement);
            ctrl.vm.autoScrollNow = false;
            ctrl.autoScrollRequested = false;
          } else if (ctrl.vm.autoScrollRequested) {
            if (ctrl.vm.path !== treePath.root) autoScroll(ctrl, vnode.elm as HTMLElement);
            ctrl.vm.autoScrollRequested = false;
          }
        },
      },
    },
    [
      ...renderChildrenOf(ctx, root, {
        parentPath: '',
        isMainline: true,
      }),
    ]
  );
}
