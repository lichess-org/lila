import type AnalyseCtrl from '../ctrl';
import type { VNode, Hooks } from 'snabbdom';
import { defined } from 'lib';
import { throttle } from 'lib/async';
import { isTouchDevice } from 'lib/device';
import { storedProp } from 'lib/storage';
import type { ConcealOf } from '../interfaces';
import { renderContextMenu } from './contextMenu';
import { renderColumnView } from './columnView';
import { renderInlineView } from './inlineView';
import { addPointerListeners } from 'lib/pointer';
import type { TreePath } from 'lib/tree/types';

export class TreeView {
  constructor(readonly ctrl: AnalyseCtrl) {}
  private autoScrollRequest: 'instant' | 'smooth' | false = false;

  hidden = true;
  modePreference = storedProp<'column' | 'inline'>(
    'treeView',
    'column',
    str => (str === 'column' ? 'column' : 'inline'),
    v => v,
  );
  mode: 'column' | 'inline';

  toggleModePreference() {
    this.modePreference(this.modePreference() === 'column' ? 'inline' : 'column');
  }

  render(concealOf?: ConcealOf): VNode {
    this.mode = concealOf ? 'column' : this.modePreference();
    return this.mode === 'column' ? renderColumnView(this.ctrl, concealOf) : renderInlineView(this.ctrl);
  }

  requestAutoScroll(request: 'instant' | 'smooth' | false) {
    this.autoScrollRequest = request;
  }

  hook(): Hooks {
    const { ctrl } = this;
    return {
      insert: vnode => {
        const el = vnode.elm as HTMLElement;
        if (ctrl.path !== '') this.autoScrollRequest = 'instant';
        const ctxMenuCallback = (e: MouseEvent) => {
          renderContextMenu(e, ctrl, eventPath(e) ?? '');
          ctrl.redraw();
          return false;
        };
        if (site.debug) {
          el.ondblclick = ctxMenuCallback; // dont steal movelist right clicks from dev tools in debug
        } else {
          el.oncontextmenu = ctxMenuCallback; // otherwise, standard prod behavior
        }
        if (isTouchDevice()) {
          el.ondblclick = ctxMenuCallback;
          addPointerListeners(el, { hold: ctxMenuCallback });
        }
        el.addEventListener('pointerup', (e: PointerEvent) => {
          if (!(e.target instanceof HTMLElement)) return;
          if (e.target.classList.contains('disclosure') || (defined(e.button) && e.button !== 0)) return;
          const path = eventPath(e);
          if (path) {
            if (e.shiftKey) {
              ctrl.setRangePoint(path);
            } else {
              ctrl.userJump(path);
            }
          }
          this.autoScrollRequest = false;
          ctrl.redraw();
        });
      },
      postpatch: () => {
        if (this.autoScrollRequest) {
          autoScroll(this.autoScrollRequest);
          this.autoScrollRequest = false;
        }
      },
    };
  }
}

function eventPath(e: MouseEvent): TreePath | null {
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
