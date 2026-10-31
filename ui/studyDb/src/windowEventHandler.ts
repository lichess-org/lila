import type { TabContent } from './tabContentCtrl';
import { moveWindow, fitWindow, windowViewport, type Rect, type ResizeEdge } from './windowBounds';
import type { WindowCtrl } from './windowCtrl';

export class WindowEventHandler {
  gesture?: {
    source: WindowCtrl;
    content?: TabContent;
    selected: string;
    edge?: ResizeEdge;
    rect: Rect;
    moved: boolean;
    capture: HTMLElement;
    pointerId: number;
    x: number;
    y: number;
    offsetX: number;
    offsetY: number;
    preview?: HTMLElement;
    ghostTab?: HTMLElement;
    target?: HTMLElement;
    destination?: WindowCtrl;
    cursorTimer?: ReturnType<typeof setTimeout>;
  };
  private releaseClick?: HTMLElement;

  start(source: WindowCtrl, event: PointerEvent): void {
    source.owner.bringToFront(source);
    if (event.button !== 0 || !event.isPrimary || this.gesture) return;
    this.releaseClick = undefined;
    const target = event.target as HTMLElement;
    source.owner.clearSelection(
      target.closest('.study-row, button, input, select, textarea, a, label, form')
        ? source.activeContentCtrl
        : undefined,
    );
    if (target.closest('.tab-close')) return;
    const button = target.closest<HTMLElement>('.tabs > button[role="tab"]');
    const edge = target.closest<HTMLElement>('[data-resize]')?.dataset.resize as ResizeEdge | undefined;
    if (!button && !edge && !target.matches('.tabs')) return;
    const content = source.tabCtrls.find(content => content.id === button?.id);
    const capture = button ?? (event.currentTarget as HTMLElement);
    const rect = capture.getBoundingClientRect();
    event.preventDefault();
    this.gesture = {
      source,
      content: content === source.tabCtrls[0] ? undefined : content,
      selected: source.selectedTabId,
      edge,
      rect: source.geometry,
      moved: false,
      capture,
      pointerId: event.pointerId,
      x: event.clientX,
      y: event.clientY,
      offsetX: event.clientX - rect.left,
      offsetY: event.clientY - rect.top,
    };
    capture.setPointerCapture(event.pointerId);
    if (button) this.gesture.cursorTimer = setTimeout(() => (capture.style.cursor = 'grabbing'), 100);
  }

  readonly move = (event: PointerEvent) => {
    const gesture = this.gesture;
    if (!gesture || gesture.pointerId !== event.pointerId) return;
    if (!gesture.moved) {
      if (!gesture.edge && Math.hypot(event.clientX - gesture.x, event.clientY - gesture.y) < 6) return;
      gesture.moved = true;
      gesture.source.windowFrame!.frame!.dialog.classList.add('dragging');
    }
    if (gesture.content && !gesture.preview) {
      gesture.source.selectTab(gesture.content.id);
      const preview = document.createElement('div');
      preview.className = 'study-db tab-preview';
      preview.setAttribute('aria-hidden', 'true');
      preview.inert = true;
      const innerContent = gesture.capture.closest('.inner-content')!.cloneNode(true) as HTMLElement;
      const tab = gesture.capture.cloneNode(true) as HTMLElement;
      tab.classList.add('active');
      innerContent.querySelector('.tabs')!.replaceChildren(tab);
      for (const resize of innerContent.querySelectorAll('.resize')) resize.remove();
      preview.append(innerContent);
      for (const element of preview.querySelectorAll('[id]')) element.removeAttribute('id');
      preview.style.width = `${gesture.source.geometry.width}px`;
      preview.style.height = `${gesture.source.geometry.height}px`;
      document.body.append(preview);
      gesture.preview = preview;
      gesture.capture.classList.add('tab-dragging');
    }
    if (gesture.preview) {
      const rect = fitWindow(
        {
          ...gesture.source.geometry,
          left: event.clientX - gesture.offsetX,
          top: event.clientY - gesture.offsetY,
        },
        windowViewport(),
      );
      gesture.preview.style.left = `${rect.left}px`;
      gesture.preview.style.top = `${rect.top}px`;
      gesture.preview.style.width = `${rect.width}px`;
      gesture.preview.style.height = `${rect.height}px`;
    } else {
      gesture.source.windowFrame!.apply(
        moveWindow(
          gesture.rect,
          event.clientX - gesture.x,
          event.clientY - gesture.y,
          gesture.edge,
          windowViewport(),
        ),
      );
    }
    if (gesture.edge) return;
    const destination = [...gesture.source.owner.windows].reverse().find(ctrl => {
      if (ctrl === gesture.source) return false;
      const frame = ctrl.windowFrame?.frame?.view.querySelector('.tabs');
      if (!frame) return false;
      const rect = frame.getBoundingClientRect();
      return (
        event.clientX >= rect.left &&
        event.clientX <= rect.right &&
        event.clientY >= rect.top &&
        event.clientY <= rect.bottom
      );
    });
    if (destination !== gesture.destination) {
      gesture.target?.classList.remove('tab-drop-target');
      gesture.ghostTab?.remove();
      gesture.destination = destination;
      gesture.target = destination?.windowFrame!.frame!.dialog;
      gesture.target?.classList.add('tab-drop-target');
      if (destination) {
        const ghostTabs = document.createElement('span');
        ghostTabs.className = 'tab-ghost';
        ghostTabs.setAttribute('aria-hidden', 'true');
        ghostTabs.inert = true;
        for (const content of gesture.content ? [gesture.content] : gesture.source.tabCtrls) {
          const ghost = gesture.source
            .windowFrame!.frame!.view.querySelector(`#${content.id}`)!
            .cloneNode(true) as HTMLElement;
          ghost.removeAttribute('id');
          ghost.removeAttribute('aria-controls');
          ghost.removeAttribute('role');
          ghost.className = 'tab-ghost';
          ghostTabs.append(ghost);
        }
        const strip = destination.windowFrame!.frame!.view.querySelector('.tabs')!;
        strip.insertBefore(ghostTabs, strip.querySelector('.window-close'));
        gesture.ghostTab = ghostTabs;
      } else gesture.ghostTab = undefined;
    }
    if (gesture.preview) gesture.preview.hidden = !!destination;
  };

  readonly end = (event: PointerEvent) => {
    const gesture = this.gesture;
    if (!gesture || gesture.pointerId !== event.pointerId) return;
    this.move(event);
    this.clear();
    if (gesture.moved) this.releaseClick = gesture.capture;
    if (gesture.moved && !gesture.edge && (gesture.content || gesture.destination)) {
      const destination = gesture.source.owner.transfer(
        gesture.source,
        gesture.content,
        gesture.destination,
        {
          ...gesture.source.geometry,
          left: event.clientX - gesture.offsetX,
          top: event.clientY - gesture.offsetY,
        },
      );
      destination.focusContent();
    } else gesture.source.owner.save();
  };

  readonly pointerCancel = (event: PointerEvent) => {
    if (event.pointerId === this.gesture?.pointerId) this.cancel();
  };

  cancel(ctrl?: WindowCtrl): boolean {
    if (!this.gesture || (ctrl && this.gesture.source !== ctrl && this.gesture.destination !== ctrl))
      return false;
    const gesture = this.gesture;
    this.clear();
    this.releaseClick = gesture.moved ? gesture.capture : undefined;
    gesture.source.windowFrame?.apply(gesture.rect);
    if (gesture.content && gesture.source.selectedTabId !== gesture.selected)
      gesture.source.selectTab(gesture.selected);
    return true;
  }

  click(source: WindowCtrl, event: MouseEvent): void {
    const button = (event.target as HTMLElement).closest<HTMLElement>('.tabs > button[role="tab"]');
    const suppressed = event.detail > 0 && button === this.releaseClick;
    this.releaseClick = undefined;
    if (!button) return;
    if (suppressed) event.preventDefault();
    else {
      const content = source.tabCtrls.find(content => content.id === button.id);
      if (content) {
        source.selectTab(content.id);
        source.focusContent();
      }
    }
  }

  keyDown(ctrl: WindowCtrl, event: KeyboardEvent): void {
    if (event.key !== 'Escape') return;
    event.preventDefault();
    event.stopPropagation();
    if (!this.cancel()) ctrl.owner.closeWindow(ctrl);
  }

  navKeyDown(ctrl: WindowCtrl, event: KeyboardEvent): void {
    if ((event.target as HTMLElement).closest('.window-close')) return;
    if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
    event.preventDefault();
    const tabIds = ctrl.tabCtrls.map(content => content.id);
    const active = ctrl.activeContentCtrl;
    ctrl.selectTab(
      tabIds[
        event.key === 'Home'
          ? 0
          : event.key === 'End'
            ? tabIds.length - 1
            : (tabIds.indexOf(active.id) + (event.key === 'ArrowRight' ? 1 : -1) + tabIds.length) %
              tabIds.length
      ],
    );
    (event.currentTarget as HTMLElement).querySelector<HTMLElement>('[aria-selected="true"]')?.focus();
  }

  clear(): void {
    const gesture = this.gesture;
    if (!gesture) return;
    this.gesture = undefined;
    clearTimeout(gesture.cursorTimer);
    gesture.capture.style.removeProperty('cursor');
    gesture.preview?.remove();
    gesture.ghostTab?.remove();
    gesture.source.windowFrame?.frame?.dialog.classList.remove('dragging');
    gesture.target?.classList.remove('tab-drop-target');
    gesture.capture.classList.remove('tab-dragging');
    if (gesture.capture.hasPointerCapture(gesture.pointerId))
      gesture.capture.releasePointerCapture(gesture.pointerId);
  }
}

export const windowEventHandler = new WindowEventHandler();
