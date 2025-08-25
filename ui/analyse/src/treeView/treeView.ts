import type AnalyseCtrl from '../ctrl';
import type { VNode, Hooks } from 'snabbdom';
import { defined } from 'lib';
import { throttle } from 'lib/async';
import { isTouchDevice, displayColumns } from 'lib/device';
import { storedProp } from 'lib/storage';
import type { ConcealOf } from '../interfaces';
import { renderContextMenu } from './contextMenu';
import { renderColumnView } from './columnView';
import { renderInlineView } from './inlineView';
import { addPointerListeners } from 'lib/pointer';

export class TreeView {
  constructor(readonly ctrl: AnalyseCtrl) {}

  hidden = true;
  modePreference = storedProp<'column' | 'inline'>(
    'treeView',
    'column',
    str => (str === 'column' ? 'column' : 'inline'),
    v => v,
  );

  toggleModePreference() {
    if (this.modePreference() === 'column') this.modePreference('inline');
    else this.modePreference('column');
  }

  render(concealOf?: ConcealOf): VNode {
    return (this.modePreference() === 'column' && displayColumns() > 1) || concealOf
      ? renderColumnView(this.ctrl, concealOf)
      : renderInlineView(this.ctrl);
  }

  hook(): Hooks {
    const { ctrl } = this;
    return {
      insert: vnode => {
        const el = vnode.elm as HTMLElement;
        if (ctrl.path !== '') ctrl.autoScrollRequested = 'instant';
        const ctxMenuCallback = (e: MouseEvent) => {
          renderContextMenu(e, ctrl, eventPath(e) ?? '');
          ctrl.redraw();
          return false;
        };
        //el.oncontextmenu = ctxMenuCallback;
        if (isTouchDevice()) {
          el.ondblclick = ctxMenuCallback;
          addPointerListeners(el, { hold: ctxMenuCallback });
        }
        el.addEventListener('mousedown', (e: MouseEvent) => {
          if (!(e.target instanceof HTMLElement)) return;
          if (e.target.classList.contains('disclosure') || (defined(e.button) && e.button !== 0)) return;
          const path = eventPath(e);
          if (path) ctrl.userJump(path);
          ctrl.autoScrollRequested = false;
          ctrl.redraw();
        });
      },
      postpatch: () => {
        if (ctrl.autoScrollRequested) {
          autoScroll(ctrl.autoScrollRequested);
          ctrl.autoScrollRequested = false;
        }
      },
    };
  }
}

function eventPath(e: MouseEvent): Tree.Path | null {
  return (
    (e.target as HTMLElement).getAttribute('p') || (e.target as HTMLElement).parentElement!.getAttribute('p')
  );
}

const autoScroll = throttle(200, (behavior: 'instant' | 'smooth' = 'instant') => {
  const scrollView = document.querySelector<HTMLElement>('.analyse__moves')!;
  const moveEl = scrollView.querySelector<HTMLElement>('.active');
  if (!moveEl) return scrollView.scrollTo({ top: 0, behavior });
  const [move, view] = [moveEl.getBoundingClientRect(), scrollView.getBoundingClientRect()];
  const visibleHeight = Math.min(view.bottom, window.innerHeight) - Math.max(view.top, 0);
  scrollView.scrollTo({
    top: scrollView.scrollTop + move.top - view.top - (visibleHeight - move.height) / 2,
    behavior,
  });
});
