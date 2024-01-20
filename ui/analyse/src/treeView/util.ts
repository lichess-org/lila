import { defined, isEmpty } from 'common/common';
import { bindMobileTapHold } from 'common/mobile';
import { makeNotation } from 'common/notation';
import { MaybeVNodes } from 'common/snabbdom';
import throttle from 'common/throttle';
import { playable } from 'game';
import { makeUsi, parseUsi, toBW } from 'shogiops/util';
import { Hooks, VNode, h } from 'snabbdom';
import { path as treePath } from 'tree';
import AnalyseCtrl from '../ctrl';
import { authorText as commentAuthorText } from '../study/studyComments';
import { enrichText, innerHTML, plyColor } from '../util';
import contextMenu from './contextMenu';

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

export interface NodeClasses {
  active: boolean;
  'context-menu': boolean;
  current: boolean;
  nongame: boolean;
  [key: string]: boolean;
}

export function nodeClasses(ctx: Ctx, node: Tree.Node, path: Tree.Path, mainline = false): NodeClasses {
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

// create move notation in reference to node or parent node
export function usiToNotation(ctx: Ctx, node: Tree.Node, parentPath: Tree.Path, text: string): string {
  const matches = text.match(/\[usi:(\d*)\.?((?:\d\w|\w\*)\d\w(?:\+|=)?)\]/g);
  if (matches?.length) {
    for (const mText of matches) {
      const match = mText.match(/usi:(\d*)\.?((?:\d\w|\w\*)\d\w(?:\+|=)?)/);
      if (match) {
        const textPlyColor = plyColor(parseInt(match[1]) || node.ply),
          useParentNode = plyColor(node.ply) !== textPlyColor,
          parentNode = ctx.ctrl.tree.nodeAtPath(parentPath),
          refNode = useParentNode ? parentNode : node,
          refSfen =
            !node.usi && useParentNode ? refNode.sfen.replace(/ (b|w) /, ' ' + toBW(textPlyColor) + ' ') : refNode.sfen, // for initial node
          moveOrDrop = match[2] && parseUsi(match[2]), // to make sure we have valid usi
          notation =
            moveOrDrop && makeNotation(refSfen, ctx.ctrl.data.game.variant.key, makeUsi(moveOrDrop), refNode.usi);
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
        hook: innerHTML(truncated, text => by + enrichText(usiToNotation(ctx, node, parentPath, text))),
      });
    })
    .filter(nonEmpty);
}

export function findCurrentPath(c: AnalyseCtrl): Tree.Path | undefined {
  return (
    (!c.synthetic && playable(c.data) && c.initialPath) || (c.retro && c.retro.current() && c.retro.current().prev.path)
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
