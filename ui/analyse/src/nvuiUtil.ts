import { plyToTurn } from 'lib/game/chess';
import { renderComments, renderSan } from 'lib/nvui/render';
import { path as treePath } from 'lib/tree/tree';
import { type VNode, enter } from 'lib/view';

import type { AnalyseNvuiContext } from '@/analyse.nvui';
import type AnalyseCtrl from '@/ctrl';

export function clickHook(main?: (el: HTMLElement) => void, post?: () => void) {
  return {
    // put unique identifying props on the button container (such as class)
    // because snabbdom WILL mix plain adjacent buttons up.
    hook: {
      insert: (vnode: VNode) => {
        const el = vnode.elm as HTMLElement;
        el.addEventListener('click', () => {
          main?.(el);
          post?.();
        });
        el.addEventListener(
          'keydown',
          enter(() => {
            main?.(el);
            post?.();
          }),
        );
      },
    },
  };
}

export function currentLineIndex(ctrl: AnalyseCtrl): { i: number; of: number } {
  if (ctrl.path === treePath.root) return { i: 1, of: 1 };
  const prevNode = ctrl.tree.parentNode(ctrl.path);
  return {
    i: prevNode.children.findIndex(node => node.id === ctrl.node.id),
    of: prevNode.children.length,
  };
}

function renderLineIndex(ctrl: AnalyseCtrl): string {
  const { i, of } = currentLineIndex(ctrl);
  return of > 1 ? `, line ${i + 1} of ${of} ,` : '';
}

export function renderCurrentNode({
  ctrl,
  moveStyle,
}: Pick<AnalyseNvuiContext, 'ctrl' | 'moveStyle'>): string {
  const node = ctrl.node;
  if (!node.san || !node.uci) return i18n.nvui.gameStart;
  return [
    plyToTurn(node.ply),
    node.ply % 2 === 1 ? i18n.site.white : i18n.site.black,
    renderSan(node.san, node.uci, moveStyle.get()),
    renderLineIndex(ctrl),
    !ctrl.retro && renderComments(node, moveStyle.get()),
  ]
    .filter(x => x)
    .join(' ')
    .trim();
}
