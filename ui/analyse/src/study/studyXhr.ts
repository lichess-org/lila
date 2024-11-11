import type { StudyChapterConfig, ReloadData } from './interfaces';
import { text as xhrText, json as xhrJson, form as xhrForm } from 'common/xhr';

export const reload = (
  baseUrl: string,
  id: string,
  chapterId?: string,
  withChapters: boolean = false,
): Promise<ReloadData> => {
  let url = `/${baseUrl}/${id}`;
  if (chapterId) url += '/' + chapterId;
  if (withChapters) url += '?chapters=1';
  return xhrJson(url);
};

export const variants = () => xhrJson('/variant', { cache: 'default' });

export const glyphs = () =>
  xhrJson(`/study/glyphs/${document.documentElement.lang}.json`, { cache: 'default' });

export const chapterConfig = (studyId: string, chapterId: string): Promise<StudyChapterConfig> =>
  xhrJson(`/study/${studyId}/${chapterId}/config`);

export const practiceComplete = (chapterId: string, nbMoves: number) =>
  xhrText(`/practice/complete/${chapterId}/${nbMoves}`, {
    method: 'POST',
  });

export const importPgn = (studyId: string, data: any) =>
  xhrText(`/study/${studyId}/import-pgn?sri=${site.sri}`, {
    method: 'POST',
    body: xhrForm(data),
  });
