import { myUserId } from 'lib/common';
import { displayColumns } from 'lib/device';
import { storedJsonProp } from 'lib/storage';

import { BookmarkTabContent } from './bookmarkTabContent';
import { HistoryTabContent } from './historyTabContent';
import { isStudyDbAvailable } from './idbStudy';
import { MemberTabContent } from './memberTabContent';
import { ResultTabContent } from './resultTabContent';
import { SearchTabContent } from './searchTabContent';
import type { TabContent } from './tabContentCtrl';
import type { StoredTab } from './types';
import { fitWindow, windowViewport, type Rect } from './windowBounds';
import { WindowCtrl } from './windowCtrl';

export class StudyDbCtrl {
  readonly tabs = {
    memberTab: new MemberTabContent(),
    bookmarkTab: new BookmarkTabContent(),
    historyTab: new HistoryTabContent(),
    searchTab: new SearchTabContent(),
  };
  private readonly windowContents = new Map<WindowCtrl, TabContent[]>();
  private readonly savedState = storedJsonProp<StoredTab[]>('study.db.state', () => []);

  get windows(): readonly WindowCtrl[] {
    return [...this.windowContents.keys()];
  }

  hasWindow(ctrl: WindowCtrl): boolean {
    return this.windowContents.has(ctrl);
  }

  clearSelection(except?: TabContent): void {
    for (const contents of this.windowContents.values())
      for (const content of contents)
        if (content !== except && 'clearSelection' in content) content.clearSelection();
  }

  mount(ctrl: WindowCtrl): void {
    if (!this.hasWindow(ctrl)) return;
    if (!this.canOpen()) return this.closeWindow(ctrl);
    this.bringToFront(ctrl);
    for (const content of ctrl.tabCtrls) content.init();
    ctrl.focusContent();
  }

  closeWindow(ctrl: WindowCtrl): void {
    const contents = this.windowContents.get(ctrl);
    if (!contents) return;
    if (this.windowContents.size === 1 && contents.length) return this.close();
    ctrl.dispose();
    this.windowContents.delete(ctrl);
    const survivors = this.windows;
    const survivor = survivors[survivors.length - 1];
    const unique = contents.filter(content => !content.closable);
    for (const content of contents) {
      if (!survivor || content.closable) content.destroy();
    }
    contents.length = 0;
    if (survivor && unique.length) {
      this.attach(survivor, unique);
      survivor.redraw();
    }
    this.save();
  }

  closeContent(ctrl: WindowCtrl, content: TabContent): void {
    const contents = this.windowContents.get(ctrl);
    if (!contents || !content.closable || !contents.includes(content)) return;
    const index = contents.indexOf(content);
    content.destroy();
    contents.splice(index, 1);
    if (!contents.length) return this.closeWindow(ctrl);
    if (ctrl.selectedTabId === content.id) ctrl.selectTab(contents[Math.min(index, contents.length - 1)].id);
    else ctrl.redraw();
    ctrl.focusContent();
    this.save();
  }

  bringToFront(ctrl: WindowCtrl): void {
    const contents = this.windowContents.get(ctrl);
    if (!contents) return;
    this.windowContents.delete(ctrl);
    this.windowContents.set(ctrl, contents);
    let order = 0;
    for (const window of this.windows) window.windowFrame?.setOrder(order++);
  }

  open(): void {
    if (!this.canOpen()) return;
    if (!this.windowContents.size) this.load();
    for (const ctrl of this.windows) ctrl.show();
  }

  close(): void {
    if (!this.windowContents.size) return;
    this.save();
    for (const ctrl of this.windows) {
      ctrl.dispose();
      for (const content of ctrl.tabCtrls) content.destroy();
    }
    this.windowContents.clear();
  }

  toggle(): void {
    if (this.windowContents.size) this.close();
    else this.open();
  }

  async search(source: WindowCtrl): Promise<void> {
    if (!this.hasWindow(source) || !this.canOpen()) return;
    const search = this.tabs.searchTab;
    if (source.activeContentCtrl !== search) return;
    if (!search.canSubmit) return;
    const results = new ResultTabContent(search.search);
    search.pending = results;
    search.message = undefined;
    search.onChange();
    await results.init();
    while (search.pending === results && !results.entries.length && results.hasMore && !results.error)
      await results.loadMore();
    if (search.pending !== results) return;
    search.pending = undefined;
    const destination = this.windows.find(ctrl => ctrl.tabCtrls.includes(search));
    if (!destination || results.error || !results.entries.length) {
      search.message = results.error ?? 'No results';
      results.destroy();
      search.onChange();
      return;
    }
    this.attach(destination, [results]);
    destination.selectTab(results.id);
    destination.focusContent();
    this.save();
  }

  transfer(
    src: WindowCtrl,
    content: TabContent | undefined,
    dest?: WindowCtrl,
    geometry: Rect = this.offsetGeometry(src),
  ): WindowCtrl {
    const contents = this.windowContents.get(src);
    if (!contents || (content && !contents.includes(content)))
      throw new Error('Content has no source window');
    if (dest && !this.hasWindow(dest)) throw new Error('dest window is closed');
    if (dest === src || (!content && !dest)) return src;
    const selected = content ?? src.activeContentCtrl;
    const moving = content ? [content] : [...contents];
    contents.splice(0, contents.length, ...contents.filter(existing => !moving.includes(existing)));
    if (dest) this.attach(dest, moving);
    else dest = this.createWindow(moving, geometry);
    dest.selectTab(selected.id);
    if (!contents.length) this.closeWindow(src);
    else {
      if (!contents.some(existing => existing.id === src.selectedTabId)) src.selectTab(contents[0].id);
      else src.redraw();
    }
    dest.show();
    this.save();
    return dest;
  }

  save(): void {
    this.savedState(this.windows.flatMap(ctrl => ctrl.freeze()));
  }

  load(): void {
    if (this.windowContents.size) return;
    let stored: StoredTab[];
    try {
      stored = this.savedState();
    } catch {
      stored = [];
    }
    if (!Array.isArray(stored)) stored = [];
    const groups = new Map<string, { geometry: Rect; contents: TabContent[]; active: Set<TabContent> }>();
    const defaults = Object.values(this.tabs);
    const seen = new Set<TabContent>();
    for (const tab of stored) {
      const geometry = tab?.geometry;
      if (
        !geometry ||
        !['left', 'top', 'width', 'height'].every((key: keyof Rect) => Number.isFinite(geometry[key])) ||
        geometry.width <= 0 ||
        geometry.height <= 0 ||
        typeof tab.active !== 'boolean' ||
        (tab.kind !== 'resultTab' && !defaults.some(content => content.kind === tab.kind))
      ) {
        continue;
      }
      let content: TabContent;
      if (tab.kind === 'resultTab') {
        if (!tab.search || typeof tab.search !== 'object' || typeof tab.search.order !== 'string') continue;
        content = new ResultTabContent(tab.search);
      } else {
        content = this.tabs[tab.kind];
      }
      if (seen.has(content)) continue;
      seen.add(content);
      const key = JSON.stringify([geometry.left, geometry.top, geometry.width, geometry.height]);
      let group = groups.get(key);
      if (!group) {
        group = { geometry: { ...geometry }, contents: [], active: new Set() };
        groups.set(key, group);
      }
      group.contents.push(content);
      if (tab.active) group.active.add(content);
    }
    if (!groups.size) {
      this.createWindow(defaults);
      return;
    }
    const first = groups.values().next().value!;
    first.contents.push(...defaults.filter(content => !seen.has(content)));
    for (const group of groups.values()) {
      const ctrl = this.createWindow(group.contents, group.geometry);
      const active = ctrl.tabCtrls.find(content => group.active.has(content));
      if (active) ctrl.selectedTabId = active.id;
    }
  }

  private createWindow(contents: TabContent[], geometry: Rect = this.initialGeometry()): WindowCtrl {
    if (!contents.length || new Set(contents).size !== contents.length)
      throw new Error('window needs distinct content instances');
    for (const content of contents) {
      if (!content.closable && !Object.values(this.tabs).some(singleton => singleton === content))
        throw new Error('singleton content must belong to the module controller');
      if (this.windows.some(ctrl => ctrl.tabCtrls.includes(content)))
        throw new Error('content already has a window');
    }
    const owned = [...contents];
    this.sortContents(owned);
    const ctrl = new WindowCtrl(this, owned, fitWindow(geometry, windowViewport()));
    this.windowContents.set(ctrl, owned);
    for (const content of owned) content.onChange = ctrl.redraw;
    return ctrl;
  }

  private attach(ctrl: WindowCtrl, incoming: readonly TabContent[]): void {
    const contents = this.windowContents.get(ctrl)!;
    contents.push(...incoming);
    this.sortContents(contents);
    for (const content of incoming) {
      content.onChange = ctrl.redraw;
      if (ctrl.windowFrame?.frame?.dialog.open) content.init();
    }
  }

  private sortContents(contents: TabContent[]): void {
    const order = [...Object.keys(this.tabs), 'resultTab'];
    contents.sort(
      (left, right) =>
        order.indexOf(left.kind) - order.indexOf(right.kind) || left.label.localeCompare(right.label),
    );
  }

  private initialGeometry(): Rect {
    const columns = displayColumns();
    const panel = document.querySelector(columns > 2 ? 'aside.analyse__side' : 'div.analyse__tools');
    const board = document.querySelector('.analyse__board');
    if (columns < 2 || !panel || !board) return { left: 24, top: 80, width: 400, height: 480 };
    const gap = parseFloat(window.getComputedStyle(document.body).getPropertyValue('---block-gap')) / 2;
    const bounds = board.getBoundingClientRect();
    if (columns === 3) {
      const chapters = document.querySelector('div.study__chapters');
      const bottom = window.innerHeight - gap;
      const top = Math.max(
        windowViewport().top,
        Math.min(
          chapters?.getBoundingClientRect().bottom ?? panel.getBoundingClientRect().top + 64,
          bottom - 480,
        ),
      );
      return { left: gap, top, width: bounds.left - 2 * gap, height: Math.max(0, bottom - top) };
    }
    const top = panel.getBoundingClientRect().top + 64;
    const left = columns > 2 ? gap : bounds.right + gap;
    const right = columns > 2 ? bounds.left - gap : document.documentElement.clientWidth - gap;
    return {
      left,
      top,
      width: right - left,
      height: Math.min(Math.max(bounds.bottom - top, 480), window.innerHeight - gap - top),
    };
  }

  private offsetGeometry(ctrl: WindowCtrl): Rect {
    return { ...ctrl.geometry, left: ctrl.geometry.left + 24, top: ctrl.geometry.top + 24 };
  }

  private canOpen(): boolean {
    return !!myUserId() && isStudyDbAvailable(location.pathname);
  }
}
