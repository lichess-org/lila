import { RelayData, LogEvent, RelayIntro } from './interfaces';
import { StudyChapter, StudyChapterRelay } from '../interfaces';
import { isFinished } from '../studyChapters';

export default class RelayCtrl {
  log: LogEvent[] = [];
  cooldown: boolean = false;
  clockInterval?: number;
  intro: RelayIntro;

  constructor(
    public data: RelayData,
    readonly send: SocketSend,
    readonly redraw: () => void,
    readonly members: any,
    chapter: StudyChapter
  ) {
    this.applyChapterRelay(chapter, chapter.relay);
    this.intro = {
      exists: !!data.markup,
      active: !!data.markup && (location.pathname.match(/\//g) || []).length < 4,
      disable: () => {
        this.intro.active = false;
      },
    };
  }

  setSync = (v: Boolean) => {
    this.send('relaySync', v);
    this.redraw();
  };

  loading = () => !this.cooldown && this.data.sync.ongoing;

  applyChapterRelay = (c: StudyChapter, r?: StudyChapterRelay) => {
    if (this.clockInterval) clearInterval(this.clockInterval);
    if (r) {
      c.relay = this.convertDate(r);
      if (!isFinished(c)) this.clockInterval = setInterval(this.redraw, 1000);
    }
  };

  private convertDate = (r: StudyChapterRelay): StudyChapterRelay => {
    if (typeof r.secondsSinceLastMove !== 'undefined' && !r.lastMoveAt) {
      r.lastMoveAt = Date.now() - r.secondsSinceLastMove * 1000;
    }
    return r;
  };

  private socketHandlers = {
    relayData: (d: RelayData) => {
      d.sync.log = this.data.sync.log;
      this.data = d;
      this.redraw();
    },
    relayLog: (event: LogEvent) => {
      if (event.id !== this.data.id) return;
      this.data.sync.log.push(event);
      this.data.sync.log = this.data.sync.log.slice(-20);
      this.cooldown = true;
      setTimeout(() => {
        this.cooldown = false;
        this.redraw();
      }, 4500);
      this.redraw();
      if (event.error) console.warn(`relay synchronisation error: ${event.error}`);
    },
  };

  socketHandler = (t: string, d: any) => {
    const handler = this.socketHandlers[t];
    if (handler && d.id === this.data.id) {
      handler(d);
      return true;
    }
    return false;
  };
}
