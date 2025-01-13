import { defined, isEmpty } from 'common/common';
import { bindMobileTapHold } from 'common/mobile';
import { enrichText, innerHTML } from 'common/rich-text';
import type { MaybeVNodes } from 'common/snabbdom';
import throttle from 'common/throttle';
import { playable } from 'game';
import { i18n } from 'i18n';
import { usiToNotation } from 'shogi/notation';
import { type Hooks, type VNode, h } from 'snabbdom';
import { path as treePath } from 'tree';
import type AnalyseCtrl from '../ctrl';
import { authorText as commentAuthorText } from '../study/study-comments';
import contextMenu from './context-menu';

export interface Ctx {
  ctrl: AnalyseCtrl;
  showComputer: boolean;
  showGlyphs: boolean;
  variant: VariantKey;
  showEval: boolean;
  truncateComments: boolean;
  currentPath: Tree.Path | undefined;
  offset: number;
}

export interface Opts {
  parentPath: Tree.Path;
  isMainline: boolean;
  inline?: Tree.Node;
  truncate?: number;
}

interface NodeClasses {
  active: boolean;
  'context-menu': boolean;
  current: boolean;
  nongame: boolean;
  [key: string]: boolean;
}

export function nodeClasses(
  ctx: Ctx,
  node: Tree.Node,
  path: Tree.Path,
  mainline = false,
): NodeClasses {
  const glyphIds = ctx.showGlyphs && node.glyphs ? node.glyphs.map(g => g.id) : [];
  return {
    active: path === ctx.ctrl.path,
    'context-menu': path === ctx.ctrl.contextMenuPath,
    current: path === ctx.currentPath,
    nongame:
      (!ctx.currentPath &&
        !!ctx.ctrl.gamePath &&
        treePath.contains(path, ctx.ctrl.gamePath) &&
        path !== ctx.ctrl.gamePath) ||
      (mainline &&
        !!ctx.ctrl.study?.data.chapter.gameLength &&
        ctx.ctrl.study?.data.chapter.gameLength < path.length / 2),
    inaccuracy: glyphIds.includes(6),
    mistake: glyphIds.includes(2),
    blunder: glyphIds.includes(4),
    'good-move': glyphIds.includes(1),
    brilliant: glyphIds.includes(3),
    interesting: glyphIds.includes(5),
  };
}

function eventPath(e: MouseEvent): Tree.Path | null {
  return (
    (e.target as HTMLElement).getAttribute('p') ||
    ((e.target as HTMLElement).parentNode as HTMLElement).getAttribute('p')
  );
}

const autoScroll: (ctrl: AnalyseCtrl, el: HTMLElement) => void = throttle(
  200,
  (ctrl: AnalyseCtrl, el: HTMLElement) => {
    const cont = el.parentNode as HTMLElement;
    if (!cont) return;
    const target = el.querySelector('.active') as HTMLElement;
    if (!target) {
      cont.scrollTop = ctrl.path ? 99999 : 0;
      return;
    }
    cont.scrollTop = target.offsetTop - cont.offsetHeight / 2 + target.offsetHeight;
  },
);

export function mainHook(ctrl: AnalyseCtrl): Hooks {
  return {
    insert: vnode => {
      const el = vnode.elm as HTMLElement;
      if (ctrl.path !== '') autoScroll(ctrl, el);
      const ctxMenuCallback = (e: MouseEvent) => {
        const path = eventPath(e);
        if (path !== null)
          contextMenu(e, {
            path,
            root: ctrl,
          });
        ctrl.redraw();
        return false;
      };
      el.oncontextmenu = ctxMenuCallback;
      bindMobileTapHold(el, ctxMenuCallback, ctrl.redraw);

      el.addEventListener('mousedown', (e: MouseEvent) => {
        if (defined(e.button) && e.button !== 0) return; // only touch or left click
        const path = eventPath(e);
        if (path) ctrl.userJump(path);
        ctrl.redraw();
      });
    },
    postpatch: (_, vnode) => {
      if (ctrl.autoScrollRequested) {
        autoScroll(ctrl, vnode.elm as HTMLElement);
        ctrl.autoScrollRequested = false;
      }
    },
  };
}

export function renderInlineCommentsOf(
  ctx: Ctx,
  node: Tree.Node,
  parentPath: Tree.Path,
): MaybeVNodes {
  if (!ctx.ctrl.showComments || isEmpty(node.comments)) return [];
  return node
    .comments!.map(comment => {
      if (comment.by === 'lishogi' && !ctx.showComputer) return;
      const by = node.comments![1]
        ? `<span class="by">${commentAuthorText(comment.by)}</span>`
        : '';
      const truncated = truncateComment(comment.text, 300, ctx);
      return h('comment', {
        hook: innerHTML(
          truncated,
          text =>
            by +
            enrichText(
              usiToNotation(
                node,
                ctx.ctrl.tree.nodeAtPath(parentPath),
                ctx.ctrl.data.game.variant.key,
                text,
              ),
            ),
        ),
      });
    })
    .filter(nonEmpty);
}

export function findCurrentPath(c: AnalyseCtrl): Tree.Path | undefined {
  return (!c.synthetic && playable(c.data) && c.initialPath) || c.retro?.current()?.prev.path;
}

export function truncateComment(text: string, len: number, ctx: Ctx): string {
  return ctx.truncateComments && text.length > len ? `${text.slice(0, len - 10)} [...]` : text;
}

export function retroLine(ctx: Ctx, node: Tree.Node, opts: Opts): VNode | undefined {
  return node.comp && ctx.ctrl.retro && ctx.ctrl.retro.hideComputerLine(node, opts.parentPath)
    ? h('line', i18n('learnFromThisMistake'))
    : undefined;
}

export const nonEmpty = (x: any): boolean => !!x;
