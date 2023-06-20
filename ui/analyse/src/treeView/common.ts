import AnalyseCtrl from '../ctrl';
import contextMenu from './contextMenu';
import throttle from 'common/throttle';
import { enrichText, innerHTML } from 'common/richText';
import { authorText as commentAuthorText } from '../study/studyComments';
import { bindMobileTapHold } from 'common/mobile';
import { h, Hooks, VNode } from 'snabbdom';
import { isEmpty, defined } from 'common';
import { MaybeVNodes } from 'common/snabbdom';
import { path as treePath } from 'tree';
import { playable } from 'game';

export const nonEmpty = (x: unknown): boolean => !!x;

export function mainHook(ctrl: AnalyseCtrl): Hooks {
  return {
    insert: vnode => {
      const el = vnode.elm as HTMLElement;
      if (ctrl.path !== '') autoScroll(ctrl, el);
      const ctxMenuCallback = (e: MouseEvent) => {
        const path = eventPath(e);
        if (path !== null) {
          contextMenu(e, {
            path,
            root: ctrl,
          });
        }
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

function eventPath(e: MouseEvent): Tree.Path | null {
  return (
    (e.target as HTMLElement).getAttribute('p') || (e.target as HTMLElement).parentElement!.getAttribute('p')
  );
}

const autoScroll = throttle(200, (ctrl: AnalyseCtrl, el: HTMLElement) => {
  const cont = el.parentElement?.parentElement;
  if (!cont) return;
  const target = el.querySelector<HTMLElement>('.active');
  if (!target) {
    cont.scrollTop = ctrl.path ? 99999 : 0;
    return;
  }
  cont.scrollTop = target.offsetTop - cont.offsetHeight / 2 + target.offsetHeight;
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
  };
}

export function findCurrentPath(c: AnalyseCtrl): Tree.Path | undefined {
  let cur;
  return (
    (!c.synthetic && playable(c.data) && c.initialPath) ||
    (c.retro && (cur = c.retro.current()) && cur.prev.path) ||
    (c.study && c.study.data.chapter.relay && c.study.data.chapter.relay.path)
  );
}

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
  maxLength: number
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
    ? h('line', ctx.ctrl.trans.noarg('learnFromThisMistake'))
    : undefined;
}

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
  inline?: Tree.Node;
  withIndex?: boolean;
  truncate?: number;
}
