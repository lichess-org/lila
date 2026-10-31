import { licon } from 'lib/licon';

import type { ResultTabContent } from './resultTabContent';
import { TabContentCtrl } from './tabContentCtrl';
import type { StudySearchOrder, StudySearchQuery } from './types';

export class SearchTabContent extends TabContentCtrl {
  readonly kind = 'searchTab';
  readonly label = 'Search';
  readonly icon = licon.Search;
  query = '';
  order: StudySearchOrder = 'relevant';
  study: NonNullable<StudySearchQuery['study']> = {};
  tags: NonNullable<NonNullable<StudySearchQuery['chapter']>['tags']> = {};
  pending?: ResultTabContent;
  message?: string;

  override destroy(): void {
    this.pending?.destroy();
    this.pending = undefined;
    super.destroy();
  }

  get search(): StudySearchQuery {
    const clean = <Fields extends object>(fields: Fields): Fields | undefined => {
      const entries = Object.entries(fields)
        .map(([key, value]) => [key, (value as string).trim()])
        .filter(([, value]) => value);
      return entries.length ? (Object.fromEntries(entries) as Fields) : undefined;
    };
    const study = clean({ ...this.study, text: this.query });
    let tags = clean(this.tags);
    if (tags) {
      if (!tags.player1 && !tags.fideId1) delete tags.color1;
      if (!tags.player2 && !tags.fideId2) delete tags.color2;
      if (!Object.keys(tags).length) tags = undefined;
    }
    return {
      order: this.order,
      ...(study && { study }),
      ...(tags && { chapter: { tags } }),
    };
  }

  get canSubmit(): boolean {
    const { study, chapter } = this.search;
    return !this.pending && !!(study || chapter);
  }
}
