import { myUserId } from 'lib/common';
import { pubsub } from 'lib/pubsub';
import { ensureOk, readNdJson, xhrHeader } from 'lib/xhr';

import { idbStudy } from './idbStudy';
import { mergeStudyMetadata } from './studyMetadata';
import type { StudyDbRequest, StudyData, StudyUpdated, SyncCache, SyncUpdate } from './types';

export class SyncCtrl {
  complete = false;
  error?: string;
  readonly studies = new Map<string, StudyData>();
  private readonly pendingUpdates = new Set<string>();
  private readonly listeners = new Set<() => void>();
  private frame?: number;
  private request?: AbortController;
  private updating = false;

  constructor() {
    pubsub.on('studyDb.updated', this.studyUpdated);
    pubsub.on('analysis.change', (_fen, path, position) => {
      if (!position) return;
      const study = this.studies.get(position.studyId);
      if (
        !study?.visit ||
        study.studyDbExcluded ||
        study.visit.chapterId !== position.chapterId ||
        (study.visit.path === path && study.visit.orientation === position.orientation)
      )
        return;
      study.visit = { ...study.visit, path, orientation: position.orientation };
      this.publish();
    });
  }

  get loading(): boolean {
    return !!this.request;
  }

  get history(): StudyData[] {
    return [...this.studies.values()]
      .filter(study => study.visit)
      .sort((left, right) => right.visit!.at - left.visit!.at || right.studyId.localeCompare(left.studyId))
      .slice(0, 500);
  }

  watch(onChange: () => void): () => void {
    this.listeners.add(onChange);
    return () => this.listeners.delete(onChange);
  }

  store(incoming: StudyData, partial = false): StudyData {
    const cached = this.studies.get(incoming.studyId);
    const visit =
      (cached?.visit?.at ?? 0) > (incoming.visit?.at ?? 0)
        ? cached?.visit
        : (incoming.visit ?? cached?.visit);
    const study = partial
      ? mergeStudyMetadata(cached, incoming)
      : cached && this.pendingUpdates.has(incoming.studyId)
        ? mergeStudyMetadata(incoming, cached)
        : {
            ...(cached && (cached.metadataRevision ?? -1) > (incoming.metadataRevision ?? -1)
              ? cached
              : incoming),
          };
    delete study.matchingChapterIds;
    delete study.matchingChapterFields;
    if (cached) for (const key of Object.keys(cached)) delete cached[key as keyof StudyData];
    const result = Object.assign(cached ?? {}, study, visit ? { visit } : {});
    this.studies.set(result.studyId, result);
    this.publish();
    return result;
  }

  async loadHistory(signal: AbortSignal): Promise<void> {
    const studies = await idbStudy.getHistory();
    if (!signal.aborted) for (const study of studies) this.store(study);
  }

  async load(signal: AbortSignal): Promise<void> {
    if (!myUserId() || signal.aborted) return;
    this.request?.abort();
    const request = (this.request = new AbortController());
    const stop = () => request.abort();
    signal.addEventListener('abort', stop, { once: true });
    const active = () => this.request === request && !request.signal.aborted;
    this.complete = false;
    this.error = undefined;
    this.publish();
    try {
      const cache = await idbStudy.getMember(batch => {
        if (!active()) return;
        for (const study of batch) this.store(study);
        this.publish();
      }, request.signal);
      if (!active()) return;
      this.complete = cache.complete;
      this.publish();
      await this.synchronize(cache, request.signal);
    } catch {
      if (active()) this.error = 'error synchronizin studies';
    } finally {
      signal.removeEventListener('abort', stop);
      if (this.request === request) {
        this.request = undefined;
        if (!signal.aborted) {
          this.publish();
          this.integrateUpdates();
        }
      }
    }
  }

  private publish(): void {
    if (this.frame !== undefined || !this.listeners.size) return;
    this.frame = requestAnimationFrame(() => {
      this.frame = undefined;
      for (const onChange of this.listeners) onChange();
    });
  }

  private readonly studyUpdated = ({ study, chapterId, path, orientation }: StudyUpdated) => {
    if (study.studyDbExcluded) return;
    const cached = this.store(study, true);
    cached.visit = { chapterId, path, orientation, at: Date.now() };
    this.pendingUpdates.add(study.studyId);
    this.publish();
    this.integrateUpdates();
  };

  private async integrateUpdates(): Promise<void> {
    if (this.loading || this.updating || !this.pendingUpdates.size) return;
    this.updating = true;
    try {
      while (this.pendingUpdates.size) {
        const studyId = this.pendingUpdates.values().next().value!;
        this.pendingUpdates.delete(studyId);
        const study = this.studies.get(studyId)!;
        await idbStudy.storeMember(study.role ? { type: 'upsert', study } : { type: 'delete', studyId });
      }
    } catch {
      this.error = 'Unable to update study cache';
    } finally {
      this.updating = false;
      this.publish();
      this.integrateUpdates();
    }
  }

  private async synchronize(cache: SyncCache, signal: AbortSignal): Promise<void> {
    signal.throwIfAborted();
    let complete = false;
    const manifest: Record<string, number> = {};
    for (const study of cache.studies) {
      const revision = study.metadataRevision;
      if (typeof revision === 'number' && Number.isSafeInteger(revision) && revision >= 0)
        manifest[study.studyId] = revision;
    }
    cache.studies.length = 0;
    const response = await ensureOk(
      await fetch('/study/db/sync', {
        method: 'POST',
        credentials: 'same-origin',
        cache: 'no-store',
        headers: { ...xhrHeader, 'Content-Type': 'application/json', Accept: 'application/x-ndjson' },
        body: JSON.stringify({ mode: 'sync', manifest } satisfies StudyDbRequest),
        signal,
      }),
    );
    await readNdJson<SyncUpdate>(response, async update => {
      signal.throwIfAborted();
      if (complete) throw new Error('Unexpected data after sync completion');
      if (update.type === 'upsert') {
        const revision = update.study?.metadataRevision;
        if (
          !/^[a-zA-Z0-9]{8}$/.test(update.study?.studyId) ||
          typeof revision !== 'number' ||
          !Number.isSafeInteger(revision) ||
          revision < 0
        )
          throw new Error('bad study sync update');
      } else if (update.type === 'delete') {
        if (!/^[a-zA-Z0-9]{8}$/.test(update.studyId)) throw new Error('bad study sync deletion');
      } else if (update.type === 'complete') {
        complete = true;
        return;
      } else throw new Error('bad study sync record');
      await idbStudy.storeMember(update);
      signal.throwIfAborted();
      if (update.type === 'upsert') this.store(update.study);
      else {
        const study = this.studies.get(update.studyId);
        if (study) study.role = undefined;
      }
      this.publish();
    });
    signal.throwIfAborted();
    if (!complete) throw new Error('Incomplete study sync');
    await idbStudy.storeMember({ type: 'complete' });
    signal.throwIfAborted();
    this.complete = true;
    this.publish();
  }
}

export const syncCtrl = new SyncCtrl();
