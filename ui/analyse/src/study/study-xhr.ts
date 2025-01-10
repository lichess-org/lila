import type { ChapterPreview, ReloadData, StudyChapterConfig } from './interfaces';

export function reload(baseUrl: string, id: string, chapterId?: string): Promise<ReloadData> {
  let url = `/${baseUrl}/${id}`;
  if (chapterId) url += `/${chapterId}`;
  return window.lishogi.xhr.json('GET', url);
}

export function chapterConfig(studyId: string, chapterId: string): Promise<StudyChapterConfig> {
  return window.lishogi.xhr.json('GET', `/study/${studyId}/${chapterId}/meta`);
}

export function practiceComplete(chapterId: string, nbMoves: number): Promise<void> {
  return window.lishogi.xhr.json('POST', `/practice/complete/${chapterId}/${nbMoves}`);
}

export function importNotation(studyId: string, data: Record<string, any>): Promise<void> {
  return window.lishogi.xhr.json(
    'POST',
    `/study/${studyId}/import-notation?sri=${window.lishogi.sri}`,
    {
      formData: data,
    },
  );
}

export function multiBoard(
  studyId: string,
  page: number,
  playing: boolean,
): Promise<Paginator<ChapterPreview>> {
  return window.lishogi.xhr.json('GET', `/study/${studyId}/multi-board`, {
    url: { page: page.toString(), playing },
  });
}
