import type { StudyNavigationRequest } from 'study-db/types';

import { json, url as makeUrl } from 'lib/xhr';

import type { AnalyseApi, AnalyseOpts } from '../interfaces';

export class StudyNavigation {
  private pending?: AbortController;

  constructor(
    private cfg: AnalyseOpts,
    private readonly api: AnalyseApi,
    private readonly connect: (cfg: AnalyseOpts) => void,
  ) {}

  async load(
    studyId: string,
    chapterId?: string,
    position: Pick<StudyNavigationRequest, 'path' | 'orientation'> = {},
  ): Promise<void> {
    if (!/^[a-zA-Z0-9]{8}$/.test(studyId) || (chapterId && !/^[a-zA-Z0-9]{8}$/.test(chapterId)))
      throw new Error('Invalid study or chapter ID');
    this.pending?.abort();
    const request = (this.pending = new AbortController());
    const url = `/study/${studyId}${chapterId ? '/' + chapterId : ''}`;
    const target = { studyId, chapterId, ...position };
    const targetUrl = makeUrl(url, { path: position.path, orientation: position.orientation });
    if (this.cfg.embed || this.cfg.practice || this.cfg.relay) {
      site.redirect(targetUrl);
      return;
    }
    try {
      if (this.cfg.study?.id === studyId && chapterId && (await this.api.setChapter(chapterId))) {
        if (request !== this.pending) return;
        if (
          (position.path === undefined && position.orientation === undefined) ||
          this.api.restoreStudyPosition(target)
        ) {
          return;
        }
      }
      if (request !== this.pending) return;
      const next = await json<AnalyseOpts | { redirect: string }>(
        makeUrl(`/study/db/load/${studyId}`, { chapterId }),
        { signal: request.signal },
      );
      if (request !== this.pending) return;
      if ('redirect' in next) {
        site.redirect(next.redirect);
        return;
      }
      if (
        next.study?.id !== studyId ||
        !next.study.chapter?.id ||
        !next.data?.treeParts ||
        !next.socketUrl ||
        typeof next.socketVersion !== 'number'
      ) {
        throw new Error('bad study response');
      }
      try {
        history.pushState({}, '', `/study/${studyId}/${next.study.chapter.id}`);
        document.title = next.study.name;
        this.connect(next);
        const ready = this.api.reload(next);
        this.cfg = next;
        await ready;
        if (request === this.pending) this.api.restoreStudyPosition(target);
      } catch (error) {
        if (request === this.pending && !request.signal.aborted) site.redirect(targetUrl);
        throw error;
      }
    } catch (error) {
      if (request !== this.pending || request.signal.aborted) return;
      throw error;
    } finally {
      if (request === this.pending) this.pending = undefined;
    }
  }
}
