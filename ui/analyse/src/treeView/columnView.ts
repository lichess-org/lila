import { isEmpty } from 'lib';
import { type VNode, type LooseVNode, type LooseVNodes, hl } from 'lib/snabbdom';
import { fixCrazySan } from 'lib/game/chess';
import { ops as treeOps } from 'lib/tree/tree';
import type AnalyseCtrl from '../ctrl';
import type { ConcealOf, Conceal } from '../interfaces';
import {
  type Ctx as BaseCtx,
  type Opts as BaseOpts,
  nodeClasses,
  renderInlineCommentsOf,
  retroLine,
  renderMove as renderMoveWithEval,
  renderIndex,
  renderGlyph,
  renderComment,
  renderingCtx,
  disclosureBtn,
  disclosureState,
  disclosureConnector,
  showConnector,
} from './components';

export function renderColumnView(ctrl: AnalyseCtrl, concealOf: ConcealOf = () => () => null): VNode {
  const ctx: Ctx = { ...renderingCtx(ctrl), concealOf };
  const root = ctrl.tree.root;
  const commentTags = renderMainlineCommentsOf(ctx, root, false, false, '');
  const blackStarts = (root.ply & 1) === 1;

  return hl('div.tview2.tview2-column', [
    !isEmpty(commentTags) && hl('interrupt', commentTags),
    blackStarts && renderIndex(root.ply, false),
    blackStarts && emptyMove(),
    renderDescendantsOf(ctx, root, { parentPath: '', isMainline: true }),
  ]);
}

interface Ctx extends BaseCtx {
  concealOf: ConcealOf;
}

interface Opts extends BaseOpts {
  isMainline: boolean;
  conceal?: Conceal;
  noConceal?: boolean;
  branch?: Tree.Node;
}

function renderSubtree(ctx: Ctx, node: Tree.Node, opts: Opts): LooseVNodes {
  const { parentPath, isMainline, noConceal } = opts;
  const path = parentPath + node.id;
  const comments = disclosureState(node) !== 'collapsed' && renderInlineCommentsOf(ctx, node, path);
  return [
    renderMove(ctx, node, opts),
    comments,
    opts.inline &&
      hl('inline', renderSubtree(ctx, opts.inline, { ...opts, withIndex: true, inline: undefined })),
    renderDescendantsOf(ctx, node, {
      parentPath: path,
      isMainline,
      noConceal,
      anchor: disclosureState(node) === 'expanded' && showConnector(comments) && 'lines',
    }),
  ];
}

function renderDescendantsOf(ctx: Ctx, parent: Tree.Node, opts: Opts): LooseVNodes {
  const filteredKids = parent.children.filter(x => ctx.showComputer || !x.comp);
  if (filteredKids.length === 0) return;
  else if (opts.isMainline) return renderMainlineDescendantsOf(ctx, parent, filteredKids, opts);
  else return renderVariationDescendantsOf(ctx, parent, filteredKids, opts);
}

function renderMainlineDescendantsOf(
  ctx: Ctx,
  parent: Tree.Node,
  [main, ...variations]: Tree.Node[],
  opts: Opts,
): LooseVNodes {
  const { parentPath } = opts;
  const path = parentPath + main.id;
  const conceal = opts.noConceal ? null : opts.conceal || ctx.concealOf(true)(path, main);
  if (conceal === 'hide') return;
  const isWhite = main.ply % 2 === 1;

  if (main.forceVariation)
    return [
      isWhite && renderIndex(main.ply, false),
      hl('move.empty', [disclosureBtn(ctx, parent, opts.parentPath), '...']),
      disclosureState(parent) !== 'collapsed' &&
        hl(
          'interrupt',
          renderLines(ctx, [main, ...variations], {
            ...opts,
            conceal,
            isMainline: false,
            noConceal: !conceal,
          }),
        ),
    ];
  const stdOpts: Opts = { parentPath, conceal, isMainline: true };
  const commentTags = renderMainlineCommentsOf(ctx, main, conceal, true, path).filter(Boolean);
  return [
    isWhite && renderIndex(main.ply, false),
    isEmpty(variations) && isEmpty(commentTags)
      ? renderSubtree(ctx, main, { ...stdOpts, parentPath })
      : [
          renderMove(ctx, main, { ...stdOpts, branch: parent }),
          disclosureState(parent) !== 'collapsed' && [
            isWhite && emptyMove(conceal),
            hl('interrupt', { class: { anchor: disclosureState(parent, true) === 'expanded' } }, [
              commentTags,
              renderLines(ctx, variations, {
                ...stdOpts,
                noConceal: !conceal,
                anchor: disclosureState(parent, true) === 'expanded' ? 'interrupt' : undefined,
              }),
            ]),
            isWhite && main.children.length > 0 && [renderIndex(main.ply, false), emptyMove(conceal)],
          ],
          renderDescendantsOf(ctx, main, { ...stdOpts, parentPath: path }),
        ],
  ];
}

function renderVariationDescendantsOf(
  ctx: Ctx,
  parent: Tree.Node,
  kids: Tree.Node[],
  opts: Opts,
): LooseVNodes {
  const [main, second, third] = kids;
  if (second && !third && !treeOps.hasBranching(second, 6))
    return renderSubtree(ctx, main, { ...opts, inline: second });
  else if ((main && !second) || disclosureState(parent) === 'collapsed')
    return renderSubtree(ctx, main, opts);
  else return renderLines(ctx, kids, opts);
}

function renderLines(ctx: Ctx, nodes: Tree.Node[], opts: Opts): LooseVNodes {
  const { parentPath, noConceal, anchor } = opts;
  return hl('lines', { class: { anchor: anchor === 'lines', single: nodes.length === 1 } }, [
    anchor && disclosureConnector(),
    nodes.map(
      n =>
        retroLine(ctx, n) ||
        hl('line', [
          hl('branch'),
          renderSubtree(ctx, n, { parentPath, isMainline: false, withIndex: true, noConceal }),
        ]),
    ),
  ]);
}

function renderMove(ctx: Ctx, node: Tree.Node, opts: Opts): VNode {
  const path = opts.parentPath + node.id;
  const classes = nodeClasses(ctx, node, path);
  if (opts.conceal) classes[opts.conceal] = true;
  return hl(
    'move',
    { attrs: { p: path }, class: classes },
    opts.isMainline
      ? renderMoveWithEval(ctx, node, opts.branch && disclosureBtn(ctx, opts.branch, opts.parentPath))
      : [
          (opts.withIndex || node.ply % 2 === 1) && renderIndex(node.ply, true),
          hl('san', fixCrazySan(node.san!)),
          node.glyphs?.map(renderGlyph),
          disclosureState(node) && disclosureBtn(ctx, node, path),
        ],
  );
}

function renderMainlineCommentsOf(
  ctx: Ctx,
  node: Tree.Node,
  conceal: Conceal,
  withColor: boolean,
  path: string,
): LooseVNode[] {
  if (!ctx.ctrl.showComments || !Array.isArray(node.comments)) return [];
  const colorClass = withColor ? (node.ply % 2 === 0 ? '.black ' : '.white ') : '';
  return node.comments.map(comment => {
    let sel = 'comment' + colorClass;
    if (comment.text.startsWith('Inaccuracy.')) sel += '.inaccuracy';
    else if (comment.text.startsWith('Mistake.')) sel += '.mistake';
    else if (comment.text.startsWith('Blunder.')) sel += '.blunder';
    if (conceal) sel += '.' + conceal;
    return renderComment(comment, node.comments!, sel, ctx, path, 400);
  });
}

function emptyMove(conceal?: Conceal): VNode {
  const c: { conceal?: true; hide?: true } = {};
  if (conceal) c[conceal] = true;
  return hl('move.empty', { class: c }, '...');
}
