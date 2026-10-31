import { focusableWithin } from 'lib/view/focus';

import type { StudyDbCtrl } from './studyDbCtrl';
import { syncCtrl } from './syncCtrl';
import type { TabContent } from './tabContentCtrl';
import type { StoredTab } from './types';
import type { Rect } from './windowBounds';
import { WindowFrame } from './windowFrame';

export class WindowCtrl {
  selectedTabId: string;
  windowFrame?: WindowFrame;
  readonly redraw = () => this.windowFrame?.redraw();
  private readonly unwatch = syncCtrl.watch(this.redraw);

  constructor(
    readonly owner: StudyDbCtrl,
    readonly tabCtrls: readonly TabContent[],
    public geometry: Rect,
  ) {
    this.selectedTabId = tabCtrls[0].id;
  }

  show(): void {
    if (this.windowFrame || !this.owner.hasWindow(this)) return;
    this.windowFrame = new WindowFrame(this);
    this.windowFrame.show();
  }

  dispose(): void {
    const windowFrame = this.windowFrame;
    this.unwatch();
    this.windowFrame = undefined;
    windowFrame?.close();
  }

  freeze(): StoredTab[] {
    return this.tabCtrls.map(contentCtrl => ({
      ...(contentCtrl.kind === 'resultTab'
        ? { kind: contentCtrl.kind, search: contentCtrl.search }
        : { kind: contentCtrl.kind }),
      active: contentCtrl.id === this.selectedTabId,
      geometry: { ...this.geometry },
    }));
  }

  setGeometry(rect: Rect): void {
    this.geometry = rect;
    this.owner.save();
  }

  get activeContentCtrl(): TabContent {
    const content = this.tabCtrls.find(content => content.id === this.selectedTabId);
    if (!content) throw new Error('no selected content');
    return content;
  }

  focusContent(): void {
    const panel = this.windowFrame?.frame?.dialog.querySelector<HTMLElement>('.content-frame');
    if (panel) focusableWithin(panel)[0]?.focus({ preventScroll: true });
  }

  selectTab(id: string): void {
    if (!this.tabCtrls.some(content => content.id === id)) throw new Error(`window has no ${id} content`);
    if (id !== this.selectedTabId) this.owner.clearSelection();
    this.selectedTabId = id;
    this.redraw();
    this.owner.save();
  }
}
