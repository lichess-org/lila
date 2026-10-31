import { licon } from 'lib/licon';

import { StudyList } from './studyList';
import type { StudyData } from './types';

export class HistoryTabContent extends StudyList {
  readonly kind = 'historyTab';
  readonly label = 'History';
  readonly icon = licon.Clock;
  error?: string;
  private request = 0;

  override get entries(): StudyData[] {
    return this.sync.history;
  }

  protected override compare(): number {
    return 0;
  }

  protected override async load(): Promise<void> {
    const signal = this.signal;
    if (!signal || signal.aborted) return;
    const request = ++this.request;
    this.error = undefined;
    try {
      await this.sync.loadHistory(signal);
    } catch {
      if (!signal.aborted && request === this.request) this.error = 'Unable to load recent studies';
    } finally {
      if (!signal.aborted && request === this.request) this.onChange();
    }
  }

  retry(): Promise<void> {
    return this.load();
  }
}
