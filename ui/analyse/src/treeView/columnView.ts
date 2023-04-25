import { isEmpty } from 'common/common';
import { notationsWithColor } from 'common/notation';
import { MaybeVNodes } from 'common/snabbdom';
import { VNode, h } from 'snabbdom';
import { ops as treeOps, path as treePath } from 'tree';
import AnalyseCtrl from '../ctrl';
import { Conceal, ConcealOf } from '../interfaces';
import * as moveView from '../moveView';
import { authorText as commentAuthorText } from '../study/studyComments';
import { enrichText, innerHTML } from '../util';
import {
  Ctx as BaseCtx,
  Opts as BaseOpts,
  findCurrentPath,
  mainHook,
  nodeClasses,
  nonEmpty,
  renderInlineCommentsOf,
  retroLine,
  truncateComment,
  usiToNotation,
} from './util';

interface Ctx extends BaseCtx {
  concealOf: ConcealOf;
}
interface Opts extends BaseOpts {
  conceal?: Conceal;
  noConceal?: boolean;
}

function renderChildrenOf(ctx: Ctx, node: Tree.Node, opts: Opts): MaybeVNodes | undefined {
  const cs = node.children,
    main = cs[0];
  if (!main) return;
  const conceal = opts.noConceal ? null : opts.conceal || ctx.concealOf(true)(opts.parentPath + main.id, main);
  if (conceal === 'hide') return;
  if (opts.isMainline) {
    const commentTags = renderMainlineCommentsOf(ctx, main, opts, conceal, true).filter(nonEmpty);
    if (!cs[1] && isEmpty(commentTags) && !main.forceVariation)
      return ([moveView.renderIndex(main.ply, ctx.ctrl.plyOffset(), false)] as MaybeVNodes).concat(
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
    return ([moveView.renderIndex(main.ply, ctx.ctrl.plyOffset(), false)] as MaybeVNodes)
      .concat(main.forceVariation ? [h('move', '')] : [renderMoveOf(ctx, main, passOpts)])
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
      .concat(mainChildren ? [] : [])
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
        retroLine(ctx, n, opts) ||
        h(
          'line',
          renderMoveAndChildrenOf(ctx, n, {
            parentPath: opts.parentPath,
            isMainline: false,
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
    classes = nodeClasses(ctx, node, path, true);
  if (opts.conceal) classes[opts.conceal as string] = true;
  return h(
    'move',
    {
      attrs: { p: path },
      class: classes,
    },
    moveView.renderMove(ctx, node, ctx.ctrl.getMovetime(node))
  );
}

function renderVariationMoveOf(ctx: Ctx, node: Tree.Node, opts: Opts): VNode {
  const path = opts.parentPath + node.id,
    colorIcon = notationsWithColor() ? '.color-icon.' + (node.ply % 2 ? 'sente' : 'gote') : '',
    content: MaybeVNodes = [
      moveView.renderIndex(node.ply, ctx.ctrl.plyOffset(), true),
      h('move-notation' + colorIcon, node.notation),
    ],
    classes = nodeClasses(ctx, node, path);
  if (opts.conceal) classes[opts.conceal as string] = true;
  if (node.glyphs) moveView.renderGlyphs(node.glyphs).forEach(g => content.push(g));
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
    .concat(renderInlineCommentsOf(ctx, node, opts.parentPath))
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
      parentPath: opts.parentPath,
      isMainline: false,
      noConceal: opts.noConceal,
      truncate: opts.truncate,
    })
  );
}

function renderMainlineCommentsOf(
  ctx: Ctx,
  node: Tree.Node,
  opts: Opts,
  conceal: Conceal,
  withColor: boolean
): MaybeVNodes {
  if (!ctx.ctrl.showComments || isEmpty(node.comments)) return [];

  const colorClass = withColor ? (node.ply % 2 ? '.sente ' : '.gote ') : '';
  const withAuthor = node.comments!.some(c => c.by !== node.comments![0].by);

  return node.comments!.map(comment => {
    if (comment.by === 'lishogi' && !ctx.showComputer) return;
    let sel = 'comment' + colorClass;
    if (comment.text.startsWith('Inaccuracy.')) sel += '.inaccuracy';
    else if (comment.text.startsWith('Mistake.')) sel += '.mistake';
    else if (comment.text.startsWith('Blunder.')) sel += '.blunder';
    if (conceal) sel += '.' + conceal;
    const by = withAuthor ? `<span class="by">${commentAuthorText(comment.by)}</span>` : '',
      truncated = truncateComment(comment.text, 400, ctx);
    return h(sel, {
      hook: innerHTML(by + truncated, text => {
        const s = text.split('</span>');
        return by + enrichText(usiToNotation(ctx, node, opts.parentPath, s[s.length - 1]));
      }),
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
    variant: ctrl.data.game.variant.key,
    showEval: ctrl.showComputer(),
    currentPath: findCurrentPath(ctrl),
    offset: ctrl.plyOffset(),
  };
  const commentTags = renderMainlineCommentsOf(ctx, root, { parentPath: '', isMainline: true }, false, false);
  return h(
    'div.tview2.tview2-column',
    {
      hook: mainHook(ctrl),
    },
    ([isEmpty(commentTags) ? null : h('interrupt', commentTags)] as MaybeVNodes).concat(
      renderChildrenOf(ctx, root, {
        parentPath: '',
        isMainline: true,
      }) || []
    )
  );
}
