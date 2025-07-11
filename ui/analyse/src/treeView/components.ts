import type AnalyseCtrl from '../ctrl';
import contextMenu from './contextMenu';
import { throttle } from 'lib/async';
import { enrichText, innerHTML } from 'lib/richText';
import { authorText as commentAuthorText } from '../study/studyComments';
import { bindMobileTapHold, isTouchDevice, isSafari } from 'lib/device';
import { type Hooks } from 'snabbdom';
import { isEmpty, defined } from 'lib';
import { type LooseVNodes, type MaybeVNodes, type VNode, hl } from 'lib/snabbdom';
import { path as treePath, ops as treeOps } from 'lib/tree/tree';
import { playable } from 'lib/game/game';
import { PlusButton, MinusButton } from 'lib/licon';
import { fixCrazySan, plyToTurn } from 'lib/game/chess';
import { view as cevalView, renderEval as normalizeEval } from 'lib/ceval/ceval';

export interface Opts {
  parentPath: Tree.Path;
  inline?: Tree.Node;
  withIndex?: boolean;
  anchor?: 'interrupt' | 'lines' | false;
}

export interface Ctx extends MoveCtx {
  ctrl: AnalyseCtrl;
  showComputer: boolean;
  truncateComments: boolean;
  currentPath: Tree.Path | undefined;
}

export function mainHook(ctrl: AnalyseCtrl): Hooks {
  return {
    insert: vnode => {
      const el = vnode.elm as HTMLElement;
      if (ctrl.path !== '') ctrl.autoScrollRequested = true;
      const ctxMenuCallback = (e: MouseEvent) => {
        contextMenu(e, { path: eventPath(e) ?? '', root: ctrl });
        ctrl.redraw();
        return false;
      };
      el.oncontextmenu = ctxMenuCallback;
      if (isTouchDevice()) el.ondblclick = ctxMenuCallback; // long press horribad
      bindMobileTapHold(el, ctxMenuCallback, ctrl.redraw);

      el.addEventListener('mousedown', (e: MouseEvent) => {
        if (!(e.target instanceof HTMLElement)) return;
        if (e.target.classList.contains('disclosure') || (defined(e.button) && e.button !== 0)) return;
        const path = eventPath(e);
        if (path) ctrl.userJump(path);
        ctrl.redraw();
      });
    },
    postpatch: (_, vnode) => {
      if (ctrl.autoScrollRequested) {
        autoScroll(vnode.elm as HTMLElement);
        ctrl.autoScrollRequested = false;
      }
    },
  };
}

export const renderingCtx = (ctrl: AnalyseCtrl): Ctx => ({
  ctrl,
  truncateComments: false,
  showComputer: ctrl.showComputer() && !ctrl.retro?.isSolving(),
  showGlyphs: (!!ctrl.study && !ctrl.study?.relay) || ctrl.showComputer(),
  showEval: ctrl.showComputer(),
  currentPath: findCurrentPath(ctrl),
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
    .filter(Boolean);
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
  const classes = { long: truncated.includes('\n') || truncated.length > 48 };
  return truncated.length < comment.text.length
    ? hl(`${sel}.truncated`, { class: classes, hook: truncatedComment(path, ctx) }, [
        hl('span', { hook: htmlHook }),
      ])
    : hl(sel, { class: classes, hook: htmlHook });
};

export function showConnector(nodes: false | MaybeVNodes): boolean {
  if (!nodes) return true;
  nodes = nodes.filter(n => n);
  if (nodes.length === 0) return true;
  if (nodes.length > 1) return false;
  if (!nodes[0] || typeof nodes[0] === 'string') return true;
  return 'data' in nodes[0] && !nodes[0].data?.class?.long;
}

export function truncateComment(text: string, len: number, ctx: Ctx) {
  return ctx.truncateComments && text.length > len ? text.slice(0, len - 10) + ' [...]' : text;
}

export function retroLine(ctx: Ctx, node: Tree.Node): VNode | undefined {
  return node.comp && ctx.ctrl.retro && ctx.ctrl.retro.hideComputerLine(node)
    ? hl('line', i18n.site.learnFromThisMistake)
    : undefined;
}

export const renderGlyph = (glyph: Tree.Glyph): VNode =>
  hl('glyph', { attrs: { title: glyph.name } }, glyph.symbol);

export const renderIndex = (ply: Ply, withDots?: boolean): VNode =>
  hl(`index.sbhint${ply}`, renderIndexText(ply, withDots));

export function renderMove(ctx: MoveCtx, node: Tree.Node, withKid?: VNode): LooseVNodes {
  const ev = cevalView.getBestEval({ client: node.ceval, server: node.eval });
  return [
    withKid,
    hl('san', fixCrazySan(node.san!)),
    ctx.showGlyphs && node.glyphs?.map(renderGlyph),
    node.shapes && node.shapes.length > 0 && hl('shapes'),
    ev &&
      ctx.showEval &&
      ((defined(ev.cp) && renderEval(normalizeEval(ev.cp))) ||
        (defined(ev.mate) && renderEval('#' + ev.mate))),
  ];
}

export const renderIndexAndMove = (ctx: MoveCtx, node: Tree.Node): LooseVNodes =>
  node.san ? [renderIndex(node.ply, ctx.withDots), renderMove(ctx, node)] : undefined;

export function disclosureBtn(ctx: Ctx, node: Tree.Node, path: Tree.Path): VNode | undefined {
  return hl('a.disclosure', {
    attrs: { 'data-icon': node.collapsed ? PlusButton : MinusButton },
    on: { click: () => ctx.ctrl.idbTree.setCollapsed(path, !node.collapsed) },
  });
}

export function disclosureState(node?: Tree.Node, isMainline = false): false | 'expanded' | 'collapsed' {
  if (!node) return false;
  return node.collapsed
    ? 'collapsed'
    : node.children[2] || (node.children[1] && (treeOps.hasBranching(node.children[1], 6) || isMainline))
      ? 'expanded'
      : false;
}

export function disclosureConnector(): VNode {
  return hl('div.connector', {
    hook: { insert: connect, update: v => setTimeout(() => connect(v)) },
  });
}

function connect(v: VNode) {
  const [el, btn] = [v.elm as HTMLElement, previousAnchorTag(v.elm)];
  if (!el || !btn || isSafari({ below: '16' })) return;
  const btnRect = btn.getBoundingClientRect();
  const anchorRect = el.closest('.anchor')!.getBoundingClientRect();
  const height = anchorRect.top - btnRect.bottom + (btnRect.height - 12) / 2;
  const btnCenter = (parseFloat(window.getComputedStyle(btn)?.paddingInlineStart || '0') + btnRect.width) / 2;
  const inlineEnd =
    document.documentElement.dir === 'rtl'
      ? btnRect.right - anchorRect.left - btnCenter
      : anchorRect.right - btnRect.left - btnCenter;

  el.style.display = 'block';
  el.style.height = `${height}px`;
  el.style.top = `${-height}px`;
  el.style.setProperty('inset-inline-end', `${inlineEnd}px`);
}

function previousAnchorTag(el: Node | null | undefined): HTMLElement | undefined {
  while (el) {
    if (el.nodeName === 'A') return el as HTMLElement;
    if (!el.previousSibling) el = el.parentNode;
    else {
      el = el.previousSibling;
      while (el.lastChild) el = el.lastChild;
    }
  }
  return undefined;
}

function eventPath(e: MouseEvent): Tree.Path | null {
  return (
    (e.target as HTMLElement).getAttribute('p') || (e.target as HTMLElement).parentElement!.getAttribute('p')
  );
}

const renderEval = (e: string): VNode => hl('eval', e.replace('-', '−'));

const renderIndexText = (ply: Ply, withDots?: boolean): string =>
  plyToTurn(ply) + (withDots ? (ply % 2 === 1 ? '.' : '...') : '');

const autoScroll = throttle(200, (moveListEl: HTMLElement) => {
  const moveEl = moveListEl.querySelector<HTMLElement>('.active');
  if (!moveEl) return;
  const [move, view] = [moveEl.getBoundingClientRect(), moveListEl.getBoundingClientRect()];
  moveListEl.scrollTo({
    top: moveListEl.scrollTop + move.top - view.top - view.height / 2 + move.height / 2,
    behavior: 'auto',
  });
});

interface MoveCtx {
  showEval: boolean;
  showGlyphs?: boolean;
  withDots?: boolean;
}
