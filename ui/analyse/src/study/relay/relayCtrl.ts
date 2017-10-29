
export default class RelayCtrl {

  data: RelayData;

  constructor(d: RelayData, readonly send: SocketSend, readonly redraw: () => void, readonly members: any, chapter: StudyChapter) {
    this.data = d;
    this.applyChapterRelay(chapter, chapter.relay);
  }

  applyChapterRelay = (c: StudyChapter, r?: StudyChapterRelay) => {
    if (this.clockInterval) clearInterval(this.clockInterval);
    if (r) {
      c.relay = this.convertDate(r);
      if (!isFinished(c)) this.clockInterval = setInterval(this.redraw, 1000);
    }
  }

  private convertDate = (r: StudyChapterRelay): StudyChapterRelay => {
    if (typeof r.secondsSinceLastMove !== 'undefined' && !r.lastMoveAt) {
      r.lastMoveAt = Date.now() - r.secondsSinceLastMove * 1000;
    }
    return r;
  }
}
