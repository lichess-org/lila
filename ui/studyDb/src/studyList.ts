import { matchesMetadata } from './studyMetadata';
import { TabContentCtrl } from './tabContentCtrl';
import { studyDragType, type StudyDrag, type StudyData } from './types';

export abstract class StudyList extends TabContentCtrl {
  private ids: string[] = [];
  readonly selected = new Set<string>();
  query = '';
  revision = 0;
  protected visible = 50;
  private anchor?: string;

  get entries(): StudyData[] {
    return this.ids.flatMap(id => this.sync.studies.get(id) ?? []);
  }

  set entries(studies: StudyData[]) {
    this.ids = [...new Set(studies.map(study => this.sync.store(study).studyId))];
  }

  select(studyId: string, event: MouseEvent | KeyboardEvent): void {
    const keys = this.results.map(study => study.studyId);
    const anchor = this.anchor ? keys.indexOf(this.anchor) : -1;
    const additive = event.ctrlKey || event.metaKey;
    if (event.shiftKey && anchor >= 0) {
      if (!additive) this.selected.clear();
      const end = keys.indexOf(studyId);
      for (const key of keys.slice(Math.min(anchor, end), Math.max(anchor, end) + 1)) this.selected.add(key);
    } else {
      const selected = this.selected.has(studyId);
      if (!additive) this.selected.clear();
      if (additive && selected) this.selected.delete(studyId);
      else this.selected.add(studyId);
      this.anchor = studyId;
    }
    this.onChange();
  }

  clearSelection(): void {
    this.anchor = undefined;
    if (!this.selected.size) return;
    this.selected.clear();
    this.onChange();
  }

  drag(studyId: string, event: DragEvent): void {
    if (!this.selected.has(studyId)) this.select(studyId, event);
    const payload: StudyDrag = {
      kind: 'studies',
      entries: this.filtered
        .filter(study => this.selected.has(study.studyId))
        .map(({ studyId, studyName }) => ({ studyId, studyName })),
    };
    event.dataTransfer!.effectAllowed = 'copy';
    event.dataTransfer!.setData(studyDragType, JSON.stringify(payload));
  }

  protected includes(_entry: StudyData): boolean {
    return true;
  }

  protected matches(entry: StudyData, query: string): boolean {
    return matchesMetadata(entry, query);
  }

  protected compare(left: StudyData, right: StudyData): number {
    return (
      right.updatedAt - left.updatedAt ||
      (left.studyId < right.studyId ? 1 : left.studyId > right.studyId ? -1 : 0)
    );
  }

  protected get filtered(): StudyData[] {
    const query = this.query.trim().toLowerCase();
    return this.entries
      .filter(entry => this.includes(entry) && (!query || this.matches(entry, query)))
      .sort((left, right) => this.compare(left, right));
  }

  get results(): StudyData[] {
    return this.filtered.slice(0, this.visible);
  }

  get hasMore(): boolean {
    return this.filtered.length > this.visible;
  }

  filterChanged(): void {
    this.selected.clear();
    this.anchor = undefined;
    this.visible = 50;
    this.revision++;
    this.onChange();
  }

  async loadMore(): Promise<void> {
    this.visible += 50;
    this.onChange();
  }
}
