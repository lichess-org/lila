import { RelayData, LogEvent } from './interfaces';
import { StudyChapter, StudyChapterRelay } from '../interfaces';

export default class RelayCtrl {

  data: RelayData;
  log: LogEvent[] = [];
  cooldown: boolean = false;
  clockInterval?: number;

  constructor(d: RelayData, readonly send: SocketSend, readonly redraw: () => void, readonly members: any, chapter: StudyChapter) {
    this.data = d;
    this.applyChapterRelay(chapter, chapter.relay);
  }

  setSync = (v: Boolean) => {
    this.send('relaySync', v);
    this.redraw();
  }

  loading = () => !this.cooldown && !!this.data.sync.seconds;

  applyChapterRelay = (c: StudyChapter, r?: StudyChapterRelay) => {
    if (this.clockInterval) clearInterval(this.clockInterval);
    if (r) {
      c.relay = this.convertDate(r);
      if (!this.isFinished(c)) this.clockInterval = setInterval(this.redraw, 1000);
    }
  }

  isFinished = (c: StudyChapter) => c.tags.find(t => t[0] === 'Result' && t[1] !== '*');

  private convertDate = (r: StudyChapterRelay): StudyChapterRelay => {
    if (typeof r.secondsSinceLastMove !== 'undefined' && !r.lastMoveAt) {
      r.lastMoveAt = Date.now() - r.secondsSinceLastMove * 1000;
    }
    return r;
  }

  private socketHandlers = {
    relayData: (d: RelayData) => {
      d.sync.log = this.data.sync.log;
      this.data = d;
      this.redraw();
    },
    relayLog: (event: LogEvent) => {
      this.data.sync.log.push(event);
      this.data.sync.log = this.data.sync.log.slice(-20);
      this.cooldown = true;
      setTimeout(() => { this.cooldown = false; this.redraw(); }, 4500);
      this.redraw();
      if (event.error) console.warn(`relay synchronisation error: ${event.error}`);
    }
  };

  socketHandler = (t: string, d: any) => {
    const handler = this.socketHandlers[t];
    if (handler && d.id === this.data.id) {
      handler(d);
      return true;
    }
    return false;
  }
}
