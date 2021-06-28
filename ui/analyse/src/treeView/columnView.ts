import { h, VNode } from 'snabbdom';
import { isEmpty } from 'common';
import { fixCrazySan } from 'chess';
import { path as treePath, ops as treeOps } from 'tree';
import * as moveView from '../moveView';
import { authorText as commentAuthorText } from '../study/studyComments';
import AnalyseCtrl from '../ctrl';
import { MaybeVNodes, ConcealOf, Conceal } from '../interfaces';
import {
  nonEmpty,
  mainHook,
  nodeClasses,
  findCurrentPath,
  renderInlineCommentsOf,
  truncateComment,
  retroLine,
  Ctx as BaseCtx,
  Opts as BaseOpts,
} from './treeView';
import { enrichText, innerHTML } from '../util';

interface Ctx extends BaseCtx {
  concealOf: ConcealOf;
}
interface Opts extends BaseOpts {
  conceal?: Conceal;
  noConceal?: boolean;
}

function emptyMove(conceal?: Conceal): VNode {
  const c: { conceal?: true; hide?: true } = {};
  if (conceal) c[conceal] = true;
  return h(
    'move.empty',
    {
      class: c,
    },
    '...'
  );
}

function renderChildrenOf(ctx: Ctx, node: Tree.Node, opts: Opts): MaybeVNodes | undefined {
  const cs = node.children,
    main = cs[0];
  if (!main) return;
  const conceal = opts.noConceal ? null : opts.conceal || ctx.concealOf(true)(opts.parentPath + main.id, main);
  if (conceal === 'hide') return;
  if (opts.isMainline) {
    const isWhite = main.ply % 2 === 1,
      commentTags = renderMainlineCommentsOf(ctx, main, conceal, true).filter(nonEmpty);
    if (!cs[1] && isEmpty(commentTags) && !main.forceVariation)
      return ((isWhite ? [moveView.renderIndex(main.ply, false)] : []) as MaybeVNodes).concat(
        renderMoveAndChildrenOf(ctx, main, {
          parentPath: opts.parentPath,
          isMainline: true,
          conceal,
        }) || []
      );
    const mainChildren = main.forceVariation
      ? undefined
      : renderChildrenOf(ctx, main, {
          parentPath: opts.parentPath + main.id,
          isMainline: true,
          conceal,
        });
    const passOpts = {
      parentPath: opts.parentPath,
      isMainline: !main.forceVariation,
      conceal,
    };
    return (isWhite ? [moveView.renderIndex(main.ply, false)] : ([] as MaybeVNodes))
      .concat(
        main.forceVariation ? [] : [renderMoveOf(ctx, main, passOpts), isWhite ? emptyMove(passOpts.conceal) : null]
      )
      .concat([
        h(
          'interrupt',
          commentTags.concat(
            renderLines(ctx, main.forceVariation ? cs : cs.slice(1), {
              parentPath: opts.parentPath,
              isMainline: passOpts.isMainline,
              conceal,
              noConceal: !conceal,
            })
          )
        ),
      ] as MaybeVNodes)
      .concat(isWhite && mainChildren ? [moveView.renderIndex(main.ply, false), emptyMove(passOpts.conceal)] : [])
      .concat(mainChildren || []);
  }
  if (!cs[1]) return renderMoveAndChildrenOf(ctx, main, opts);
  return renderInlined(ctx, cs, opts) || [renderLines(ctx, cs, opts)];
}

function renderInlined(ctx: Ctx, nodes: Tree.Node[], opts: Opts): MaybeVNodes | undefined {
  // only 2 branches
  if (!nodes[1] || nodes[2]) return;
  // only if second branch has no sub-branches
  if (treeOps.hasBranching(nodes[1], 6)) return;
  return renderMoveAndChildrenOf(ctx, nodes[0], {
    parentPath: opts.parentPath,
    isMainline: false,
    noConceal: opts.noConceal,
    inline: nodes[1],
  });
}

function renderLines(ctx: Ctx, nodes: Tree.Node[], opts: Opts): VNode {
  return h(
    'lines',
    {
      class: { single: !nodes[1] },
    },
    nodes.map(n => {
      return (
        retroLine(ctx, n) ||
        h(
          'line',
          renderMoveAndChildrenOf(ctx, n, {
            parentPath: opts.parentPath,
            isMainline: false,
            withIndex: true,
            noConceal: opts.noConceal,
            truncate: n.comp && !treePath.contains(ctx.ctrl.path, opts.parentPath + n.id) ? 3 : undefined,
          })
        )
      );
    })
  );
}

function renderMoveOf(ctx: Ctx, node: Tree.Node, opts: Opts): VNode {
  return opts.isMainline ? renderMainlineMoveOf(ctx, node, opts) : renderVariationMoveOf(ctx, node, opts);
}

function renderMainlineMoveOf(ctx: Ctx, node: Tree.Node, opts: Opts): VNode {
  const path = opts.parentPath + node.id,
    classes = nodeClasses(ctx, node, path);
  if (opts.conceal) classes[opts.conceal as string] = true;
  return h(
    'move',
    {
      attrs: { p: path },
      class: classes,
    },
    moveView.renderMove(ctx, node)
  );
}

function renderVariationMoveOf(ctx: Ctx, node: Tree.Node, opts: Opts): VNode {
  const withIndex = opts.withIndex || node.ply % 2 === 1,
    path = opts.parentPath + node.id,
    content: MaybeVNodes = [withIndex ? moveView.renderIndex(node.ply, true) : null, fixCrazySan(node.san!)],
    classes = nodeClasses(ctx, node, path);
  if (opts.conceal) classes[opts.conceal as string] = true;
  if (node.glyphs) node.glyphs.forEach(g => content.push(moveView.renderGlyph(g)));
  return h(
    'move',
    {
      attrs: { p: path },
      class: classes,
    },
    content
  );
}

function renderMoveAndChildrenOf(ctx: Ctx, node: Tree.Node, opts: Opts): MaybeVNodes {
  const path = opts.parentPath + node.id;
  if (opts.truncate === 0)
    return [
      h(
        'move',
        {
          attrs: { p: path },
        },
        [h('index', '[...]')]
      ),
    ];
  return ([renderMoveOf(ctx, node, opts)] as MaybeVNodes)
    .concat(renderInlineCommentsOf(ctx, node))
    .concat(opts.inline ? renderInline(ctx, opts.inline, opts) : null)
    .concat(
      renderChildrenOf(ctx, node, {
        parentPath: path,
        isMainline: opts.isMainline,
        noConceal: opts.noConceal,
        truncate: opts.truncate ? opts.truncate - 1 : undefined,
      }) || []
    );
}

function renderInline(ctx: Ctx, node: Tree.Node, opts: Opts): VNode {
  return h(
    'inline',
    renderMoveAndChildrenOf(ctx, node, {
      withIndex: true,
      parentPath: opts.parentPath,
      isMainline: false,
      noConceal: opts.noConceal,
      truncate: opts.truncate,
    })
  );
}

function renderMainlineCommentsOf(ctx: Ctx, node: Tree.Node, conceal: Conceal, withColor: boolean): MaybeVNodes {
  if (!ctx.ctrl.showComments || isEmpty(node.comments)) return [];

  const colorClass = withColor ? (node.ply % 2 === 0 ? '.black ' : '.white ') : '';

  return node.comments!.map(comment => {
    if (comment.by === 'lichess' && !ctx.showComputer) return;
    let sel = 'comment' + colorClass;
    if (comment.text.startsWith('Inaccuracy.')) sel += '.inaccuracy';
    else if (comment.text.startsWith('Mistake.')) sel += '.mistake';
    else if (comment.text.startsWith('Blunder.')) sel += '.blunder';
    if (conceal) sel += '.' + conceal;
    const by = node.comments![1] ? `<span class="by">${commentAuthorText(comment.by)}</span>` : '',
      truncated = truncateComment(comment.text, 400, ctx);
    return h(sel, {
      hook: innerHTML(truncated, text => by + enrichText(text)),
    });
  });
}

const emptyConcealOf: ConcealOf = function () {
  return function () {
    return null;
  };
};

export default function (ctrl: AnalyseCtrl, concealOf?: ConcealOf): VNode {
  const root = ctrl.tree.root;
  const ctx: Ctx = {
    ctrl,
    truncateComments: !ctrl.embed,
    concealOf: concealOf || emptyConcealOf,
    showComputer: ctrl.showComputer() && !ctrl.retro,
    showGlyphs: !!ctrl.study || ctrl.showComputer(),
    showEval: ctrl.showComputer(),
    currentPath: findCurrentPath(ctrl),
  };
  const commentTags = renderMainlineCommentsOf(ctx, root, false, false);
  return h(
    'div.tview2.tview2-column',
    {
      hook: mainHook(ctrl),
    },
    (
      [
        isEmpty(commentTags) ? null : h('interrupt', commentTags),
        root.ply & 1 ? moveView.renderIndex(root.ply, false) : null,
        root.ply & 1 ? emptyMove() : null,
      ] as MaybeVNodes
    ).concat(
      renderChildrenOf(ctx, root, {
        parentPath: '',
        isMainline: true,
      }) || []
    )
  );
}
