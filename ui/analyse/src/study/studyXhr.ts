import { text as xhrText, json as xhrJson, form as xhrForm, textRaw as xhrRaw, ensureOk } from 'lib/xhr';

import type { StudyChapterConfig, ReloadData, ChapterData } from './interfaces';

export const reload = (
  baseUrl: string,
  id: string,
  chapterId?: string,
  withChapters = false,
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

export const importPgn = async (
  studyId: string,
  data: ChapterData & Record<string, string | boolean | null | undefined>,
) => {
  const res = await xhrRaw(`/study/${studyId}/import-pgn?sri=${site.sri}`, {
    method: 'POST',
    body: xhrForm(data),
  });
  return ensureOk(res).then(r => r.text());
};
