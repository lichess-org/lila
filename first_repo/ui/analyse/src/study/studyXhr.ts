import { StudyChapterConfig, ReloadData } from './interfaces';
import * as xhr from 'common/xhr';

export const reload = (baseUrl: string, id: string, chapterId?: string): Promise<ReloadData> => {
  let url = '/' + baseUrl + '/' + id;
  if (chapterId) url += '/' + chapterId;
  return xhr.json(url);
};

export const variants = () => xhr.json('/variant', { cache: 'default' });

export const glyphs = () =>
  xhr.json(lichess.assetUrl('glyphs.json', { noVersion: true }), {
    cache: 'default',
    headers: {},
  });

export const chapterConfig = (studyId: string, chapterId: string): Promise<StudyChapterConfig> =>
  xhr.json(`/study/${studyId}/${chapterId}/meta`);

export const practiceComplete = (chapterId: string, nbMoves: number) =>
  xhr.json(`/practice/complete/${chapterId}/${nbMoves}`, {
    method: 'POST',
  });

export const importPgn = (studyId: string, data: any) =>
  xhr.json(`/study/${studyId}/import-pgn?sri=${lichess.sri}`, {
    method: 'POST',
    body: xhr.form(data),
  });

export const multiBoard = (studyId: string, page: number, playing: boolean) =>
  xhr.json(`/study/${studyId}/multi-board?page=${page}&playing=${playing}`);
