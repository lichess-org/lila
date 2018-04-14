const headers = {
  'Accept': 'application/vnd.lichess.v3+json'
};

export function reload(baseUrl: string, id: string, chapterId?: string) {
  let url = '/' + baseUrl + '/' + id;
  if (chapterId) url += '/' + chapterId;
  return $.ajax({
    url,
    headers
  });
}

export function variants() {
  return $.ajax({
    url: '/variant',
    headers,
    cache: true
  });
}

export function glyphs() {
  return $.ajax({
    url: window.lichess.assetUrl('/assets/glyphs.json', { noVersion: true }),
    headers,
    cache: true
  });
}

export function chapterConfig(studyId: string, chapterId: string) {
  return $.ajax({
    url: ['/study', studyId, chapterId, 'meta'].join('/'),
    headers
  });
}

export function practiceComplete(chapterId: string, nbMoves: number) {
  return $.ajax({
    method: 'POST',
    url: ['/practice/complete', chapterId, nbMoves].join('/'),
    headers
  });
}
