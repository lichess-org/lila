import { memoize, myUserId } from 'lib/common';
import { objectStorage, type ObjectStorage } from 'lib/objectStorage';
import { pubsub } from 'lib/pubsub';
import type { TreePath } from 'lib/tree/types';
import type { StudyOrientationMeta } from 'lib/types';

import { mergeStudyMetadata } from './studyMetadata';
import type { StudyData, StudyUpdated, SyncCache, SyncRecord, SyncUpdate } from './types';

const historyLimit = 500;

export function isStudyDbAvailable(pathname: string): boolean {
  return !/^\/(?:broadcast|practice|learn)(?:\/|$)/.test(pathname);
}

export class IdbStudy {
  private readonly historyDb = memoize(() =>
    objectStorage<StudyData, string>({
      db: `study-history-${myUserId()}`,
      store: 'study-history',
      version: 2,
      upgrade: (_event, store) => store?.clear(),
    }),
  );
  private readonly memberDb = memoize(() =>
    objectStorage<SyncRecord, string>({
      db: `study-db-${myUserId()}`,
      store: 'study-db',
      version: 2,
      indices: [{ name: 'updatedAt', keyPath: ['study.updatedAt', 'study.studyId'] }],
    }),
  );
  private writes: Promise<unknown> = Promise.resolve();

  getHistory = (): Promise<StudyData[]> =>
    this.write(async () => {
      const db = await this.historyDb();
      await this.pruneHistory(db);
      return (await db.getMany()).filter(entry => !entry.studyDbExcluded);
    });

  updateStudy = async ({
    study,
    chapterId,
    path,
    orientation,
  }: StudyUpdated): Promise<string | undefined> => {
    if (!myUserId() || study.studyDbExcluded || !isStudyDbAvailable(location.pathname)) return undefined;
    const snapshot: StudyData = structuredClone({
      ...study,
      visit: { chapterId, path, orientation, at: Date.now() },
    });
    return this.write(async () => {
      const db = await this.historyDb();
      const previous = await db.get(snapshot.studyId);
      const id = await db.put(snapshot.studyId, mergeStudyMetadata(previous, snapshot));
      await this.pruneHistory(db);
      return id;
    });
  };

  getMember = async (
    onBatch?: (studies: SyncCache['studies']) => void,
    signal?: AbortSignal,
  ): Promise<SyncCache> => {
    const db = await this.memberDb();
    const studies: SyncCache['studies'] = [];
    let batch: SyncCache['studies'] = [];
    for await (const cursor of db.cursor('readonly', { index: 'updatedAt', dir: 'prev' })) {
      if (signal?.aborted) break;
      const record = cursor.value as Extract<SyncRecord, { type: 'upsert' }>;
      studies.push(record.study);
      if (onBatch) {
        batch.push(record.study);
        if (batch.length === 250) {
          onBatch(batch);
          batch = [];
        }
      }
    }
    if (batch.length && !signal?.aborted) onBatch?.(batch);
    return {
      studies,
      complete: (await db.get(''))?.type === 'complete',
    };
  };

  storeMember = async (update: SyncUpdate): Promise<void> => {
    const db = await this.memberDb();
    if (update.type === 'delete') await db.remove(update.studyId);
    else await db.put(update.type === 'upsert' ? update.study.studyId : '', update);
  };

  updatePosition = async (path: TreePath, position: StudyOrientationMeta): Promise<void> => {
    if (!myUserId() || !isStudyDbAvailable(location.pathname)) return;
    await this.write(async () => {
      const db = await this.historyDb();
      const entry = await db.get(position.studyId);
      if (
        !entry ||
        entry.studyDbExcluded ||
        !entry.visit ||
        entry.visit.chapterId !== position.chapterId ||
        (entry.visit.path === path && entry.visit.orientation === position.orientation)
      )
        return;
      await db.put(position.studyId, {
        ...entry,
        visit: { ...entry.visit, path, orientation: position.orientation },
      });
    });
  };

  private async pruneHistory(db: ObjectStorage<StudyData, string>): Promise<void> {
    if ((await db.count()) <= historyLimit) return;
    const entries = await db.getMany();
    entries.sort(
      (left, right) =>
        (right.visit?.at ?? 0) - (left.visit?.at ?? 0) || right.studyId.localeCompare(left.studyId),
    );
    await Promise.all(entries.slice(historyLimit).map(entry => db.remove(entry.studyId)));
  }

  private write<Value>(operation: () => Promise<Value>): Promise<Value> {
    const result = this.writes.then(operation);
    this.writes = result.catch(() => {});
    return result;
  }
}

export const idbStudy: IdbStudy = new IdbStudy();

pubsub.on('analysis.change', (_fen, path, study) => {
  if (study) idbStudy.updatePosition(path, study).catch(() => {});
});
