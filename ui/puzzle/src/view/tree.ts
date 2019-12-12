import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { defined } from 'common';
import throttle from 'common/throttle';
import { renderEval as normalizeEval } from 'chess';
import { path as treePath } from 'tree';
import { MaybeVNodes } from '../interfaces';

const autoScroll = throttle(150, (ctrl, el) => {
  var cont = el.parentNode;
  var target = el.querySelector('.active');
  if (!target) {
    cont.scrollTop = ctrl.vm.path === treePath.root ? 0 : 99999;
    return;
  }
  cont.scrollTop = target.offsetTop - cont.offsetHeight / 2 + target.offsetHeight;
});

function pathContains(ctx, path) {
  return treePath.contains(ctx.ctrl.vm.path, path);
}

function plyToTurn(ply) {
  return Math.floor((ply - 1) / 2) + 1;
}

export function renderIndex(ply, withDots): VNode {
  return h('index', plyToTurn(ply) + (withDots ? (ply % 2 === 1 ? '.' : '...') : ''));
}

function renderChildrenOf(ctx, node, opts): MaybeVNodes {
  const cs = node.children, main = cs[0];
  if (!main) return [];
  if (opts.isMainline) {
    const isWhite = main.ply % 2 === 1;
    if (!cs[1]) return [
      isWhite ? renderIndex(main.ply, false) : null,
      ...renderMoveAndChildrenOf(ctx, main, {
        parentPath: opts.parentPath,
        isMainline: true
      })
    ];
    const mainChildren = renderChildrenOf(ctx, main, {
      parentPath: opts.parentPath + main.id,
      isMainline: true
    }),
    passOpts = {
      parentPath: opts.parentPath,
      isMainline: true
    };
    return [
      isWhite ? renderIndex(main.ply, false) : null,
      renderMoveOf(ctx, main, passOpts),
      isWhite ? emptyMove() : null,
      h('interrupt', renderLines(ctx, cs.slice(1), {
        parentPath: opts.parentPath,
        isMainline: true
      })),
      ...(isWhite && mainChildren ? [
        renderIndex(main.ply, false),
        emptyMove()
      ] : []),
      ...mainChildren
    ];
  }
  return cs[1] ? [renderLines(ctx, cs, opts)] : renderMoveAndChildrenOf(ctx, main, opts);
}

function renderLines(ctx, nodes, opts): VNode {
  return h('lines', {
    class: { single: !!nodes[1] }
  }, nodes.map(function(n) {
    return h('line', renderMoveAndChildrenOf(ctx, n, {
      parentPath: opts.parentPath,
      isMainline: false,
      withIndex: true
    }));
  }));
}

function renderMoveOf(ctx, node, opts): VNode {
  return opts.isMainline ? renderMainlineMoveOf(ctx, node, opts) : renderVariationMoveOf(ctx, node, opts);
}

function renderMainlineMoveOf(ctx, node, opts): VNode {
  const path = opts.parentPath + node.id;
  const classes: any = {
    active: path === ctx.ctrl.vm.path,
    current: path === ctx.ctrl.vm.initialPath,
    hist: node.ply < ctx.ctrl.vm.initialNode.ply
  };
  if (node.puzzle) classes[node.puzzle] = true;
  return h('move', {
    attrs: { p: path },
    class: classes
  }, renderMove(ctx, node));
}

function renderGlyph(glyph): VNode {
  return h('glyph', {
    attrs: { title: glyph.name }
  }, glyph.symbol);
}

function puzzleGlyph(ctx, node): VNode | undefined {
  switch (node.puzzle) {
    case 'good':
    case 'win':
      return renderGlyph({
        name: ctx.ctrl.trans.noarg('bestMove'),
        symbol: '✓'
      });
 case 'fail':
   return renderGlyph({
     name: ctx.ctrl.trans.noarg('puzzleFailed'),
     symbol: '✗'
   });
 case 'retry':
   return renderGlyph({
     name: ctx.ctrl.trans.noarg('goodMove'),
     symbol: '?!'
   });
  }
}

export function renderMove(ctx, node): MaybeVNodes {
  const ev = node.eval || node.ceval || {};
  return [
    node.san,
    defined(ev.cp) ? renderEval(normalizeEval(ev.cp)) : (
      defined(ev.mate) ? renderEval('#' + ev.mate) : null
    ),
    puzzleGlyph(ctx, node)
  ];
}

function renderVariationMoveOf(ctx, node, opts): VNode {
  const withIndex = opts.withIndex || node.ply % 2 === 1;
  const path = opts.parentPath + node.id;
  const active = path === ctx.ctrl.vm.path;
  const classes: any = {
    active,
    parent: !active && pathContains(ctx, path)
  };
  if (node.puzzle) classes[node.puzzle] = true;
  return h('move', {
    attrs: { p: path},
    class: classes
  }, [
    withIndex ? renderIndex(node.ply, true) : null,
    node.san,
    puzzleGlyph(ctx, node)
  ]);
}

function renderMoveAndChildrenOf(ctx, node, opts): MaybeVNodes {
  return [
    renderMoveOf(ctx, node, opts),
    ...renderChildrenOf(ctx, node, {
      parentPath: opts.parentPath + node.id,
      isMainline: opts.isMainline
    })
  ];
}

function emptyMove() {
  return h('move.empty', '...');
}

function renderEval(e) {
  return h('eval', e);
}

function eventPath(e) {
  return e.target.getAttribute('p') || e.target.parentNode.getAttribute('p');
}

export function render(ctrl): VNode {
  const root = ctrl.getTree().root;
  const ctx = {
    ctrl: ctrl,
    showComputer: false
  };
  return h('div.tview2.tview2-column', {
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
        }
        else if (ctrl.vm.autoScrollRequested) {
          if (ctrl.vm.path !== treePath.root) autoScroll(ctrl, vnode.elm as HTMLElement);
          ctrl.vm.autoScrollRequested = false;
        }
      }
    }
  }, [
    ...(root.ply % 2 === 1 ? [
      renderIndex(root.ply, false),
      emptyMove()
    ] : []),
    ...renderChildrenOf(ctx, root, {
      parentPath: '',
      isMainline: true
    })
  ]);
}
