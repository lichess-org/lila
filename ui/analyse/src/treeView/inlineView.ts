import { hl, type VNode, type LooseVNodes } from 'lib/snabbdom';
import { fixCrazySan } from 'lib/game/chess';
import { ops as treeOps } from 'lib/tree/tree';
import type AnalyseCtrl from '../ctrl';
import {
  nodeClasses,
  renderInlineCommentsOf,
  renderIndex,
  renderGlyph,
  retroLine,
  Ctx,
  Opts,
  renderingCtx,
  disclosureBtn,
  disclosureConnector,
  disclosureState,
} from './components';

export function renderInlineView(ctrl: AnalyseCtrl): VNode {
  const ctx = renderingCtx(ctrl);
  ctrl.tree.root.collapsed = false;
  return hl('div.tview2.tview2-inline', [
    renderInlineCommentsOf(ctx, ctrl.tree.root, ''),
    renderDescendantsOf(ctx, ctrl.tree.root, { parentPath: '' }),
  ]);
}

function renderDescendantsOf(ctx: Ctx, parent: Tree.Node, opts: Opts): LooseVNodes {
  const { parentPath, anchor } = opts;
  const kids = parent.children.filter(x => ctx.showComputer || !x.comp);
  if (kids.length === 0) return;
  const [main, second, third] = kids;

  if (second && !third && !treeOps.hasBranching(second, 6))
    return renderSubtree(ctx, main, { ...opts, inline: second });
  else if ((main && !second) || disclosureState(parent) === 'collapsed')
    return renderSubtree(ctx, main, opts);
  else
    return hl('lines', { class: { anchor: anchor === 'lines' } }, [
      anchor && disclosureConnector(),
      kids.map(
        n =>
          retroLine(ctx, n) ||
          hl('line', [hl('branch'), renderSubtree(ctx, n, { parentPath, withIndex: true })]),
      ),
    ]);
}

function renderSubtree(ctx: Ctx, node: Tree.Node, opts: Opts): LooseVNodes {
  const { parentPath, inline } = opts;
  const path = parentPath + node.id;
  return [
    renderMove(ctx, node, opts),
    disclosureState(node) !== 'collapsed' && renderInlineCommentsOf(ctx, node, path),
    inline && hl('inline', renderSubtree(ctx, inline, { parentPath, withIndex: true })),
    renderDescendantsOf(ctx, node, {
      parentPath: path,
      anchor: disclosureState(node) === 'expanded' && 'lines',
    }),
  ];
}

function renderMove(ctx: Ctx, node: Tree.Node, opts: Opts): VNode {
  const path = opts.parentPath + node.id;

  return hl('move', { attrs: { p: path }, class: nodeClasses(ctx, node, path) }, [
    (opts.withIndex || node.ply % 2 === 1) && renderIndex(node.ply, true),
    hl('san', fixCrazySan(node.san!)),
    node.glyphs?.map(renderGlyph),
    disclosureState(node) && disclosureBtn(ctx, node, path),
  ]);
}
