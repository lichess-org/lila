import type AnalyseCtrl from '../ctrl';
import contextMenu from './contextMenu';
import { throttle } from 'lib/async';
import { enrichText, innerHTML } from 'lib/richText';
import { authorText as commentAuthorText } from '../study/studyComments';
import { addPointerListeners, isTouchDevice } from 'lib/device';
import { h, type Hooks, type VNode } from 'snabbdom';
import { isEmpty, defined } from 'lib';
import { type MaybeVNodes } from 'lib/snabbdom';
import { path as treePath } from 'lib/tree/tree';
import { playable } from 'lib/game/game';

export const nonEmpty = (x: unknown): boolean => !!x;

export function mainHook(ctrl: AnalyseCtrl): Hooks {
  return {
    insert: vnode => {
      const el = vnode.elm as HTMLElement;
      if (ctrl.path !== '') autoScroll();
      const ctxMenuCallback = (e: MouseEvent) => {
        const path = eventPath(e);
        if (path !== null) contextMenu(e, { path, root: ctrl });
        ctrl.redraw();
        return false;
      };

      el.oncontextmenu = ctxMenuCallback;
      if (isTouchDevice()) addPointerListeners(el, undefined, ctxMenuCallback);

      el.addEventListener('pointerdown', (e: PointerEvent) => {
        if (defined(e.button) && e.button !== 0) return; // only touch or left click
        const path = eventPath(e);
        if (path) ctrl.userJump(path);
        ctrl.redraw();
      });
    },
    postpatch: () => {
      if (ctrl.autoScrollRequested) {
        autoScroll();
        ctrl.autoScrollRequested = false;
      }
    },
  };
}

function eventPath(e: MouseEvent): Tree.Path | null {
  return (
    (e.target as HTMLElement).getAttribute('p') || (e.target as HTMLElement).parentElement!.getAttribute('p')
  );
}

const autoScroll = throttle(200, () => {
  const scrollView = document.querySelector<HTMLElement>('.analyse__moves')!;
  const moveEl = scrollView.querySelector<HTMLElement>('.active');
  if (!moveEl) return scrollView.scrollTo({ top: 0, behavior: 'auto' });

  const [move, view] = [moveEl.getBoundingClientRect(), scrollView.getBoundingClientRect()];
  const visibleHeight = Math.min(view.bottom, window.innerHeight) - Math.max(view.top, 0);

  scrollView.scrollTo({
    top: scrollView.scrollTop + move.top - view.top - (visibleHeight - move.height) / 2,
    behavior: 'auto',
  });
});

export interface NodeClasses {
  active: boolean;
  'context-menu': boolean;
  current: boolean;
  nongame: boolean;
  [key: string]: boolean;
}

export function nodeClasses(ctx: Ctx, node: Tree.Node, path: Tree.Path): NodeClasses {
  const glyphIds = ctx.showGlyphs && node.glyphs ? node.glyphs.map(g => g.id) : [];
  return {
    active: path === ctx.ctrl.path,
    'context-menu': path === ctx.ctrl.contextMenuPath,
    current: path === ctx.currentPath,
    nongame:
      !ctx.currentPath &&
      !!ctx.ctrl.gamePath &&
      treePath.contains(path, ctx.ctrl.gamePath) &&
      path !== ctx.ctrl.gamePath,
    inaccuracy: glyphIds.includes(6),
    mistake: glyphIds.includes(2),
    blunder: glyphIds.includes(4),
    good: glyphIds.includes(1),
    brilliant: glyphIds.includes(3),
    interesting: glyphIds.includes(5),
    'pending-deletion': path.startsWith(ctx.ctrl.pendingDeletionPath() || ' '),
    'pending-copy': !!ctx.ctrl.pendingCopyPath()?.startsWith(path),
  };
}

export const findCurrentPath = (c: AnalyseCtrl): Tree.Path | undefined =>
  (!c.synthetic && playable(c.data) && c.initialPath) ||
  c.retro?.current()?.prev.path ||
  c.study?.data.chapter.relayPath;

export const truncatedComment = (path: string, ctx: Ctx): Hooks => ({
  insert(vnode: VNode) {
    (vnode.elm as HTMLElement).addEventListener('click', () => {
      ctx.ctrl.userJumpIfCan(path);
      // Select the comments tab in the underboard
      ctx.ctrl.study?.vm.toolTab('comments');
      //Redraw everything
      ctx.ctrl.redraw();
      // Scroll down to the comments tab
      $('.analyse__underboard')[0]?.scrollIntoView();
    });
  },
});

export function renderInlineCommentsOf(ctx: Ctx, node: Tree.Node, path: string): MaybeVNodes {
  if (!ctx.ctrl.showComments || isEmpty(node.comments)) return [];
  return node
    .comments!.map(comment => renderComment(comment, node.comments!, 'comment', ctx, path, 300))
    .filter(nonEmpty);
}

export const renderComment = (
  comment: Tree.Comment,
  others: Tree.Comment[],
  sel: string,
  ctx: Ctx,
  path: string,
  maxLength: number,
) => {
  if (comment.by === 'lichess' && !ctx.showComputer) return;
  const by = !others[1] ? '' : `<span class="by">${commentAuthorText(comment.by)}</span>`,
    truncated = truncateComment(comment.text, maxLength, ctx),
    htmlHook = innerHTML(truncated, text => by + enrichText(text));
  return truncated.length < comment.text.length
    ? h(`${sel}.truncated`, { hook: truncatedComment(path, ctx) }, [h('span', { hook: htmlHook })])
    : h(sel, { hook: htmlHook });
};

export function truncateComment(text: string, len: number, ctx: Ctx) {
  return ctx.truncateComments && text.length > len ? text.slice(0, len - 10) + ' [...]' : text;
}

export function retroLine(ctx: Ctx, node: Tree.Node): VNode | undefined {
  return node.comp && ctx.ctrl.retro && ctx.ctrl.retro.hideComputerLine(node)
    ? h('line', i18n.site.learnFromThisMistake)
    : undefined;
}

export const renderingCtx = (ctrl: AnalyseCtrl): Ctx => ({
  ctrl,
  truncateComments: false,
  showComputer: ctrl.showComputer() && !ctrl.retro?.isSolving(),
  showGlyphs: (!!ctrl.study && !ctrl.study?.relay) || ctrl.showComputer(),
  showEval: ctrl.showComputer(),
  currentPath: findCurrentPath(ctrl),
});

export interface Ctx {
  ctrl: AnalyseCtrl;
  showComputer: boolean;
  showGlyphs: boolean;
  showEval: boolean;
  truncateComments: boolean;
  currentPath: Tree.Path | undefined;
}

export interface Opts {
  parentPath: Tree.Path;
  isMainline: boolean;
  depth: number;
  inline?: Tree.Node;
  withIndex?: boolean;
  truncate?: number;
}
