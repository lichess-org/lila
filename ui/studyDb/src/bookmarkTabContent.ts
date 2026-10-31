import { licon } from 'lib/licon';
import { json, text } from 'lib/xhr';

import { matchesMetadata } from './studyMetadata';
import { TabContentCtrl } from './tabContentCtrl';
import type { BookmarkEntry, BookmarkFolder, StudyDrag, StudyData } from './types';

interface BookmarkData {
  entries: BookmarkEntry[];
  studies: StudyData[];
}

export class BookmarkTabContent extends TabContentCtrl {
  readonly kind = 'bookmarkTab';
  readonly label = 'Bookmarks';
  readonly icon = licon.Book;
  entries: BookmarkEntry[] = [];
  readonly studies = this.sync.studies;
  readonly selected = new Set<BookmarkEntry>();
  readonly expanded = new Set<BookmarkFolder>();
  query = '';
  revision = 0;
  editing?: { entry: BookmarkEntry; name: string };
  error?: { message: string; retry: () => Promise<void> };
  private visible = 50;
  private anchor?: BookmarkEntry;
  private writes: Promise<void> = Promise.resolve();

  protected override async load(): Promise<void> {
    const signal = this.signal!;
    try {
      await this.writes;
      const { entries, studies } = await json<BookmarkData>('/study/db/bookmarks', { signal });
      if (signal.aborted) return;
      this.entries = entries;
      for (const study of studies) this.sync.store(study);
      this.selected.clear();
      this.expanded.clear();
      this.editing = undefined;
      this.anchor = undefined;
      this.error = undefined;
    } catch {
      if (!signal.aborted) this.error = { message: 'Unable to load bookmarks', retry: () => this.load() };
    }
    if (!signal.aborted) this.onChange();
  }

  get rows(): { entry: BookmarkEntry; path: number[] }[] {
    const query = this.query.trim().toLowerCase();
    const visit = (entries: BookmarkEntry[], parent: number[]): typeof this.rows =>
      entries.flatMap((entry, index) => {
        const path = [...parent, index];
        const children = 'children' in entry ? visit(entry.children, path) : [];
        const metadata = 'children' in entry ? entry.name : [entry, this.studies.get(entry.studyId)];
        if (query && !matchesMetadata(metadata, query) && !children.length) return [];
        return [
          { entry, path },
          ...('children' in entry && (query || this.expanded.has(entry)) ? children : []),
        ];
      });
    return visit(this.entries, []);
  }

  get results(): typeof this.rows {
    return this.rows.slice(0, this.visible);
  }

  get hasMore(): boolean {
    return this.rows.length > this.visible;
  }

  filterChanged(): void {
    this.visible = 50;
    this.revision++;
    this.onChange();
  }

  async loadMore(): Promise<void> {
    this.visible += 50;
    this.onChange();
  }

  select(entry: BookmarkEntry, event?: MouseEvent | KeyboardEvent): void {
    const additive = event?.ctrlKey || event?.metaKey;
    if (event?.shiftKey && this.anchor) {
      const entries = this.rows.map(row => row.entry);
      const start = entries.indexOf(this.anchor);
      const end = entries.indexOf(entry);
      if (!additive) this.selected.clear();
      for (const selected of entries.slice(Math.min(start, end), Math.max(start, end) + 1))
        this.selected.add(selected);
    } else {
      const selected = this.selected.has(entry);
      if (!additive) this.selected.clear();
      if (additive && selected) this.selected.delete(entry);
      else this.selected.add(entry);
      this.anchor = entry;
    }
    this.onChange();
  }

  clearSelection(): void {
    this.anchor = undefined;
    if (!this.selected.size) return;
    this.selected.clear();
    this.onChange();
  }

  toggle(folder: BookmarkFolder): void {
    if (this.expanded.has(folder)) this.expanded.delete(folder);
    else this.expanded.add(folder);
    this.onChange();
  }

  createFolder(): void {
    const selected = [...this.selected];
    const parent = selected.length === 1 && 'children' in selected[0] ? selected[0] : undefined;
    const folder: BookmarkFolder = { name: 'New folder', children: [] };
    (parent?.children ?? this.entries).push(folder);
    if (parent) this.expanded.add(parent);
    this.selected.clear();
    this.selected.add(folder);
    this.editing = { entry: folder, name: folder.name };
    this.save();
  }

  rename(entry: BookmarkEntry): void {
    this.editing = { entry, name: entry.name };
    this.onChange();
  }

  finishRename(): void {
    const entry = this.editing!.entry;
    entry.name = this.editing!.name.trim() || entry.name;
    this.editing = undefined;
    this.save();
  }

  removeSelected(): void {
    const prune = (entries: BookmarkEntry[]): BookmarkEntry[] =>
      entries
        .filter(entry => !this.selected.has(entry))
        .map(entry => {
          if ('children' in entry) entry.children = prune(entry.children);
          return entry;
        });
    this.entries = prune(this.entries);
    this.selected.clear();
    this.save();
  }

  drag(entry: BookmarkEntry): StudyDrag {
    if (!this.selected.has(entry)) this.select(entry);
    const paths: number[][] = [];
    const visit = (entries: BookmarkEntry[], parent: number[]) => {
      entries.forEach((entry, index) => {
        const path = [...parent, index];
        if (this.selected.has(entry)) paths.push(path);
        else if ('children' in entry) visit(entry.children, path);
      });
    };
    visit(this.entries, []);
    return { kind: 'bookmarks', paths };
  }

  private canDrop(payload: StudyDrag, parent?: BookmarkFolder): boolean {
    return (
      !parent ||
      payload.kind === 'studies' ||
      !payload.paths.some(path => this.contains(this.at(path), parent))
    );
  }

  drop(payload: StudyDrag, parent?: BookmarkFolder, before?: BookmarkEntry): void {
    if (!this.canDrop(payload, parent)) return;
    let incoming: BookmarkEntry[];
    if (payload.kind === 'bookmarks') {
      incoming = payload.paths.map(path => this.at(path));
      if (incoming.includes(before!)) return;
      for (const entry of incoming) {
        const siblings = this.siblings(entry)!;
        siblings.splice(siblings.indexOf(entry), 1);
      }
    } else
      incoming = [
        ...new Map(
          payload.entries.map(({ studyId, studyName }) => [studyId, { name: studyName, studyId, studyName }]),
        ).values(),
      ];
    const siblings = parent?.children ?? this.entries;
    siblings.splice(before ? siblings.indexOf(before) : siblings.length, 0, ...incoming);
    if (parent) this.expanded.add(parent);
    this.selected.clear();
    for (const entry of incoming) this.selected.add(entry);
    this.save();
  }

  save(): Promise<void> {
    const body = JSON.stringify(this.entries);
    this.onChange();
    this.writes = this.writes.then(async () => {
      try {
        await text('/study/db/bookmarks', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body,
        });
        const { studies } = await json<BookmarkData>('/study/db/bookmarks', { signal: this.signal });
        if (this.signal?.aborted) return;
        for (const study of studies) this.sync.store(study);
        this.error = undefined;
      } catch {
        this.error = { message: 'Unable to save bookmarks', retry: () => this.save() };
      }
      this.onChange();
    });
    return this.writes;
  }

  at(path: number[]): BookmarkEntry {
    let entries = this.entries;
    let entry!: BookmarkEntry;
    for (const index of path) {
      entry = entries[index];
      if ('children' in entry) entries = entry.children;
    }
    return entry;
  }

  private contains(entry: BookmarkEntry, target: BookmarkEntry): boolean {
    return (
      entry === target || ('children' in entry && entry.children.some(child => this.contains(child, target)))
    );
  }

  private siblings(target: BookmarkEntry, entries = this.entries): BookmarkEntry[] | undefined {
    if (entries.includes(target)) return entries;
    for (const entry of entries) {
      if ('children' in entry) {
        const siblings = this.siblings(target, entry.children);
        if (siblings) return siblings;
      }
    }
    return undefined;
  }
}
