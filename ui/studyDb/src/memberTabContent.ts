import { licon } from 'lib/licon';

import { StudyList } from './studyList';
import type { StudyData } from './types';

export class MemberTabContent extends StudyList {
  readonly kind = 'memberTab';
  readonly label = 'Member';
  readonly icon = licon.User;
  filters: { role: NonNullable<StudyData['role']>; private: boolean } = { role: 'member', private: false };

  override get entries(): StudyData[] {
    return [...this.sync.studies.values()].filter(study => study.role);
  }

  get loading(): boolean {
    return this.sync.loading;
  }

  get exhausted(): boolean {
    return this.sync.complete;
  }

  get error(): string | undefined {
    return this.sync.error;
  }

  protected override includes(entry: StudyData): boolean {
    const permissions = { member: 0, contributor: 1, owner: 2 };
    return (
      !entry.studyDbExcluded &&
      entry.role !== undefined &&
      permissions[entry.role] >= permissions[this.filters.role] &&
      (!this.filters.private || entry.visibility === 'private')
    );
  }

  protected override load(): Promise<void> {
    return this.sync.load(this.signal!);
  }

  retry(): Promise<void> {
    return this.loading ? Promise.resolve() : this.load();
  }
}
