import { h, Hooks, VNode } from 'snabbdom';
import { defined, isEmpty } from 'common/common';
import { makeNotation } from 'common/notation';
import throttle from 'common/throttle';
import { makeUsi, parseUsi } from 'shogiops/util';
import { path as treePath } from 'tree';
import AnalyseCtrl from '../ctrl';
import contextMenu from './contextMenu';
import { bindMobileTapHold, enrichText, innerHTML } from '../util';
import { authorText as commentAuthorText } from '../study/studyComments';
import { MaybeVNodes } from 'common/snabbdom';
import { playable } from 'game';

export interface Ctx {
  ctrl: AnalyseCtrl;
  showComputer: boolean;
  showGlyphs: boolean;
  notation: number;
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

export interface NodeClasses {
  active: boolean;
  'context-menu': boolean;
  current: boolean;
  nongame: boolean;
  [key: string]: boolean;
}

export function nodeClasses(ctx: Ctx, path: Tree.Path): NodeClasses {
  return {
    active: path === ctx.ctrl.path,
    'context-menu': path === ctx.ctrl.contextMenuPath,
    current: path === ctx.currentPath,
    nongame:
      !ctx.currentPath &&
      !!ctx.ctrl.gamePath &&
      treePath.contains(path, ctx.ctrl.gamePath) &&
      path !== ctx.ctrl.gamePath,
  };
}

function eventPath(e: MouseEvent): Tree.Path | null {
  return (
    (e.target as HTMLElement).getAttribute('p') ||
    ((e.target as HTMLElement).parentNode as HTMLElement).getAttribute('p')
  );
}

export const autoScroll = throttle(200, (ctrl: AnalyseCtrl, el: HTMLElement) => {
  const cont = el.parentNode as HTMLElement;
  if (!cont) return;
  const target = el.querySelector('.active') as HTMLElement;
  if (!target) {
    cont.scrollTop = ctrl.path ? 99999 : 0;
    return;
  }
  cont.scrollTop = target.offsetTop - cont.offsetHeight / 2 + target.offsetHeight;
});

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

export function usiToNotation(ctx: Ctx, node: Tree.Node, parentPath: Tree.Path, text: string): string {
  const matches = text.match(/\[usi:(\d*)\.?(\d\w\d\w)\]/g);
  if (matches?.length) {
    for (const mText of matches) {
      const match = mText.match(/usi:(\d*)\.?(\d\w\d\w)/);
      if (match) {
        // the default is that the move is played after this node
        const plyCutoff = node.ply - (parseInt(match[1]) || node.ply),
          parent = plyCutoff <= 0 ? node : ctx.ctrl.tree.nodeAtPath(parentPath.slice(0, -2 * plyCutoff)),
          md = match[2] && parseUsi(match[2]), // to make sure we have valid usi
          notation =
            md &&
            parent &&
            makeNotation(
              ctx.ctrl.data.pref.notation,
              parent.sfen,
              ctx.ctrl.data.game.variant.key,
              makeUsi(md),
              parent.usi
            );
        if (notation) text = text.replace(mText, notation);
        else text = text.replace(mText, 'Invalid move');
      }
    }
  }
  return text;
}

export function renderInlineCommentsOf(ctx: Ctx, node: Tree.Node, parentPath: Tree.Path): MaybeVNodes {
  if (!ctx.ctrl.showComments || isEmpty(node.comments)) return [];
  return node
    .comments!.map(comment => {
      if (comment.by === 'lishogi' && !ctx.showComputer) return;
      const by = node.comments![1] ? `<span class="by">${commentAuthorText(comment.by)}</span>` : '',
        truncated = truncateComment(comment.text, 300, ctx);
      return h('comment', {
        hook: innerHTML(usiToNotation(ctx, node, parentPath, truncated), text => by + enrichText(text)),
      });
    })
    .filter(nonEmpty);
}

export function findCurrentPath(c: AnalyseCtrl): Tree.Path | undefined {
  return (
    (!c.synthetic && playable(c.data) && c.initialPath) ||
    (c.retro && c.retro.current() && c.retro.current().prev.path) ||
    (c.study && c.study.data.chapter.relay && c.study.data.chapter.relay.path)
  );
}

export function truncateComment(text: string, len: number, ctx: Ctx): string {
  return ctx.truncateComments && text.length > len ? text.slice(0, len - 10) + ' [...]' : text;
}

export function retroLine(ctx: Ctx, node: Tree.Node, opts: Opts): VNode | undefined {
  return node.comp && ctx.ctrl.retro && ctx.ctrl.retro.hideComputerLine(node, opts.parentPath)
    ? h('line', ctx.ctrl.trans.noarg('learnFromThisMistake'))
    : undefined;
}

export const nonEmpty = (x: any): boolean => !!x;
