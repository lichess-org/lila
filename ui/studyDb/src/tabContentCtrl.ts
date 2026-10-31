import type { LiconValue } from 'lib/licon';

import type { BookmarkTabContent } from './bookmarkTabContent';
import type { HistoryTabContent } from './historyTabContent';
import type { MemberTabContent } from './memberTabContent';
import type { ResultTabContent } from './resultTabContent';
import type { SearchTabContent } from './searchTabContent';
import { syncCtrl } from './syncCtrl';
import type { TabStorage } from './types';

export type TabContent =
  | HistoryTabContent
  | MemberTabContent
  | BookmarkTabContent
  | ResultTabContent
  | SearchTabContent;

let nextId = 0;

export abstract class TabContentCtrl {
  abstract readonly kind: TabStorage['kind'];
  abstract readonly label: string;
  abstract readonly icon?: LiconValue;
  readonly id = `study-db-tab-${nextId++}`;
  readonly closable: boolean = false;
  readonly sync = syncCtrl;
  protected signal?: AbortSignal;
  private lifetime?: AbortController;
  private loaded = false;

  async init(): Promise<void> {
    if (this.lifetime) return;

    this.lifetime = new AbortController();
    this.signal = this.lifetime.signal;
    if (this.loaded) return;

    const signal = this.signal;
    await this.load();
    if (!signal.aborted) this.loaded = true;
  }

  destroy(): void {
    this.lifetime?.abort();
    this.lifetime = undefined;
    this.onChange = () => {};
  }

  onChange: () => void = () => {};

  protected load(): void | Promise<void> {}
}
