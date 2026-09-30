import { licon } from 'lib/licon';
import { jsx, snabDialog, type Dialog, type VNode } from 'lib/view';

import { bookmarkView } from './bookmarkView';
import { historyView } from './historyView';
import { memberView } from './memberView';
import { searchView } from './searchView';
import { studyListView } from './studyListView';
import type { TabContent } from './tabContentCtrl';
import { studyDragType, type StudyDrag } from './types';
import { fitWindow, resizeEdges, windowViewport, type Rect } from './windowBounds';
import type { WindowCtrl } from './windowCtrl';
import { windowEventHandler } from './windowEventHandler';

function contentView(content: TabContent, submit: () => void): VNode | undefined {
  switch (content.kind) {
    case 'historyTab':
      return historyView(content);
    case 'memberTab':
      return memberView(content);
    case 'bookmarkTab':
      return bookmarkView(content);
    case 'searchTab':
      return searchView(content, submit);
    case 'resultTab':
      return studyListView(content);
    default:
      return undefined;
  }
}

export class WindowFrame {
  frame?: Dialog<WindowCtrl>;

  constructor(readonly ctrl: WindowCtrl) {}

  async show(): Promise<void> {
    const frame = await snabDialog<WindowCtrl>({
      ctx: this.ctrl,
      class: 'study-db',
      css: [{ key: 'studyDb' }],
      modal: false,
      noTabCycle: true,
      focus: '.content-frame input',
      attrs: { dialog: { 'aria-label': 'Study database', 'pass-ctrl': true } },
      onShow: frame => {
        if (this.ctrl.windowFrame !== this) return frame.close();
        window.addEventListener('resize', this.fit);
        this.ctrl.owner.mount(this.ctrl);
      },
      onClose: () => {
        if (this.ctrl.windowFrame === this) this.ctrl.owner.closeWindow(this.ctrl);
      },
      render: this.render,
    });
    if (this.ctrl.windowFrame !== this) return frame.dialog.remove();

    this.frame = frame;
    this.fit();
    frame.show();
  }

  readonly redraw = () => {
    if (!this.frame) return;
    this.frame.redraw();
    this.apply(this.ctrl.geometry);
  };

  close(): void {
    windowEventHandler.cancel(this.ctrl);
    window.removeEventListener('resize', this.fit);
    this.frame?.close('cancel');
    this.frame = undefined;
  }

  setOrder(order: number): void {
    this.frame?.dialog.style.setProperty('---study-db-frame-order', String(order));
  }

  readonly fit = () => {
    windowEventHandler.cancel(this.ctrl);
    this.apply(fitWindow(this.ctrl.geometry, windowViewport()));
  };

  apply(rect: Rect): void {
    const tabs = this.frame!.view.querySelector('.tabs')!;
    const width = [...tabs.children].reduce((sum, tab) => sum + tab.getBoundingClientRect().width, 0);
    rect = fitWindow({ ...rect, width: Math.max(rect.width, Math.ceil(width)) }, windowViewport());
    this.ctrl.setGeometry(rect);
    Object.assign(this.frame!.dialog.style, {
      left: `${rect.left}px`,
      top: `${rect.top}px`,
      width: `${rect.width}px`,
      height: `${rect.height}px`,
    });
  }

  readonly pointerDown = (event: PointerEvent) => windowEventHandler.start(this.ctrl, event);

  private readonly render = () => {
    if (this.ctrl.windowFrame !== this) return [];

    const { ctrl } = this;
    const active = ctrl.activeContentCtrl;
    const bookmarks = ctrl.owner.tabs.bookmarkTab;
    return (
      <div
        class="inner-content"
        on={{
          pointerdown: this.pointerDown,
          pointermove: windowEventHandler.move,
          pointerup: windowEventHandler.end,
          pointercancel: windowEventHandler.pointerCancel,
          lostpointercapture: windowEventHandler.pointerCancel,
          click: (event: MouseEvent) => windowEventHandler.click(ctrl, event),
          keydown: (event: KeyboardEvent) => windowEventHandler.keyDown(ctrl, event),
        }}>
        <div
          class="tabs"
          attrs={{ role: 'tablist', 'aria-label': 'Study lists' }}
          on={{ keydown: (event: KeyboardEvent) => windowEventHandler.navKeyDown(ctrl, event) }}>
          {ctrl.tabCtrls.map(content => (
            <button
              key={content.id}
              class={[active === content && 'active']}
              attrs={{
                type: 'button',
                role: 'tab',
                id: content.id,
                title: content.label,
                'aria-selected': String(active === content),
                tabindex: active === content ? -1 : 0,
              }}
              on={{
                mousedown: (event: MouseEvent) => event.preventDefault(),
                dragover: (event: DragEvent) => {
                  if (content !== bookmarks || !event.dataTransfer!.types.includes(studyDragType)) return;
                  event.preventDefault();
                  ctrl.owner.bringToFront(ctrl);
                  if (ctrl.selectedTabId !== content.id) ctrl.selectTab(content.id);
                },
                drop: (event: DragEvent) => {
                  if (content !== bookmarks || !event.dataTransfer!.types.includes(studyDragType)) return;
                  event.preventDefault();
                  bookmarks.drop(JSON.parse(event.dataTransfer!.getData(studyDragType)) as StudyDrag);
                },
              }}>
              {content.icon ? (
                <span class="tab-icon">{content.icon}</span>
              ) : (
                <span class="tab-label">{content.label}</span>
              )}
              {content.closable && ctrl.tabCtrls.length > 1 && (
                <span
                  class="tab-close"
                  attrs={{
                    role: 'button',
                    tabindex: 0,
                    title: 'Close tab',
                    'aria-label': `Close ${content.label}`,
                    'data-icon': licon.X,
                  }}
                  on={{
                    click: (event: MouseEvent) => {
                      event.stopPropagation();
                      ctrl.owner.closeContent(ctrl, content);
                    },
                    keydown: (event: KeyboardEvent) => {
                      if (!['Enter', ' '].includes(event.key)) return;
                      event.preventDefault();
                      event.stopPropagation();
                      ctrl.owner.closeContent(ctrl, content);
                    },
                  }}
                />
              )}
            </button>
          ))}
          <button
            key="window-close"
            class="window-close"
            attrs={{
              type: 'button',
              title: 'Close window',
              'aria-label': 'Close window',
              'data-icon': licon.X,
            }}
            on={{ click: () => ctrl.owner.closeWindow(ctrl) }}
          />
        </div>
        <div class="content-frame">{contentView(active, () => ctrl.owner.search(ctrl))}</div>
        {resizeEdges.map(edge => (
          <span class={['resize', 'resize-' + edge]} attrs={{ 'data-resize': edge, 'aria-hidden': 'true' }} />
        ))}
      </div>
    );
  };
}
