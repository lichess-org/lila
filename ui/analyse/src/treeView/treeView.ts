import AnalyseCtrl from '../ctrl';
import column from './columnView';
import contextMenu from './contextMenu';
import inline from './inlineView';
import isCol1 from 'common/isCol1';
import throttle from 'common/throttle';
import { authorText as commentAuthorText } from '../study/studyComments';
import { enrichText, innerHTML, bindMobileTapHold } from '../util';
import { h, Hooks, VNode } from 'snabbdom';
import { isEmpty, defined } from 'common';
import { MaybeVNodes, ConcealOf } from '../interfaces';
import { path as treePath } from 'tree';
import { playable } from 'game';
import { storedProp, StoredProp } from 'common/storage';

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

export interface NodeClasses {
  active: boolean;
  'context-menu': boolean;
  current: boolean;
  nongame: boolean;
  [key: string]: boolean;
}

export type TreeViewKey = 'column' | 'inline';

export interface TreeView {
  get: StoredProp<TreeViewKey>;
  set(inline: boolean): void;
  toggle(): void;
  inline(): boolean;
}

export function ctrl(initialValue: TreeViewKey = 'column'): TreeView {
  const value = storedProp<TreeViewKey>('treeView', initialValue);
  function inline() {
    return value() === 'inline';
  }
  function set(i: boolean) {
    value(i ? 'inline' : 'column');
  }
  return {
    get: value,
    set,
    toggle() {
      set(!inline());
    },
    inline,
  };
}

// entry point, dispatching to selected view
export function render(ctrl: AnalyseCtrl, concealOf?: ConcealOf): VNode {
  return (ctrl.treeView.inline() || isCol1()) && !concealOf ? inline(ctrl) : column(ctrl, concealOf);
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

export function renderInlineCommentsOf(ctx: Ctx, node: Tree.Node): MaybeVNodes {
  if (!ctx.ctrl.showComments || isEmpty(node.comments)) return [];
  return node
    .comments!.map(comment => {
      if (comment.by === 'lichess' && !ctx.showComputer) return;
      const by = node.comments![1] ? `<span class="by">${commentAuthorText(comment.by)}</span>` : '',
        truncated = truncateComment(comment.text, 300, ctx);
      return h('comment', {
        hook: innerHTML(truncated, text => by + enrichText(text)),
      });
    })
    .filter(nonEmpty);
}

export function truncateComment(text: string, len: number, ctx: Ctx) {
  return ctx.truncateComments && text.length > len ? text.slice(0, len - 10) + ' [...]' : text;
}

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

export function retroLine(ctx: Ctx, node: Tree.Node): VNode | undefined {
  return node.comp && ctx.ctrl.retro && ctx.ctrl.retro.hideComputerLine(node)
    ? h('line', ctx.ctrl.trans.noarg('learnFromThisMistake'))
    : undefined;
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

export const nonEmpty = (x: unknown): boolean => !!x;
