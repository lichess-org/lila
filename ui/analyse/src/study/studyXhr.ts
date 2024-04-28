import { StudyChapterConfig, ReloadData } from './interfaces';
import * as xhr from 'common/xhr';

export const reload = (baseUrl: string, id: string, chapterId?: string): Promise<ReloadData> => {
  let url = `/${baseUrl}/${id}`;
  if (chapterId) url += '/' + chapterId;
  return xhr.json(url);
};

export const variants = () => xhr.json('/variant', { cache: 'default' });

export const glyphs = () =>
  xhr.json(`/study/glyphs/${document.documentElement.lang}.json`, { cache: 'default' });

export const chapterConfig = (studyId: string, chapterId: string): Promise<StudyChapterConfig> =>
  xhr.json(`/study/${studyId}/${chapterId}/config`);

export const practiceComplete = (chapterId: string, nbMoves: number) =>
  xhr.text(`/practice/complete/${chapterId}/${nbMoves}`, {
    method: 'POST',
  });

export const importPgn = (studyId: string, data: any) =>
  xhr.text(`/study/${studyId}/import-pgn?sri=${site.sri}`, {
    method: 'POST',
    body: xhr.form(data),
  });
