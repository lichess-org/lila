import type { StudyChapterConfig, ReloadData } from './interfaces';
import { text as xhrText, json as xhrJson, form as xhrForm, textRaw as xhrRaw } from 'lib/xhr';

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

export const importPgn = async (studyId: string, data: any) => {
  const res = await xhrRaw(`/study/${studyId}/import-pgn?sri=${site.sri}`, {
    method: 'POST',
    body: xhrForm(data),
  });
  if (res.ok) return res.text();
  if (res.status === 429) throw new Error('Too many requests');
  if (res.status === 413) throw new Error('The uploaded file is too large');
  if (res.status === 400) {
    const text = await res.text();
    throw new Error(text);
  }
  throw new Error(`Error ${res.status}`);
};
