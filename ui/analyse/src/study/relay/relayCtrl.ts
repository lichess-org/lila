import { RelayData, LogEvent, RelaySync, RelayRound } from './interfaces';
import { RelayTab, StudyChapter, StudyChapterRelay } from '../interfaces';
import { isFinished } from '../studyChapters';
import { StudyMemberCtrl } from '../studyMembers';
import { AnalyseSocketSend } from '../../socket';
import { Toggle, prop, toggle } from 'common';

export default class RelayCtrl {
  log: LogEvent[] = [];
  cooldown = false;
  clockInterval?: number;
  tourShow: Toggle;
  tab = prop<RelayTab>('overview');

  constructor(
    public id: string,
    public data: RelayData,
    readonly send: AnalyseSocketSend,
    readonly redraw: () => void,
    readonly members: StudyMemberCtrl,
    chapter: StudyChapter,
  ) {
    this.applyChapterRelay(chapter, chapter.relay);
    this.tourShow = toggle((location.pathname.match(/\//g) || []).length < 5);
  }

  setSync = (v: boolean) => {
    this.send('relaySync', v);
    this.redraw();
  };

  loading = () => !this.cooldown && this.data.sync?.ongoing;

  applyChapterRelay = (c: StudyChapter, r?: StudyChapterRelay) => {
    if (this.clockInterval) clearInterval(this.clockInterval);
    if (r) {
      c.relay = this.convertDate(r);
      if (!isFinished(c)) this.clockInterval = setInterval(this.redraw, 1000);
    }
  };

  roundById = (id: string) => this.data.rounds.find(r => r.id == id);
  currentRound = () => this.roundById(this.id)!;

  fullRoundName = () => `${this.data.tour.name} - ${this.currentRound().name}`;

  tourPath = () => `/broadcast/${this.data.tour.slug}/${this.data.tour.id}`;
  roundPath = (round?: RelayRound) => {
    const r = round || this.currentRound();
    return r && `/broadcast/${this.data.tour.slug}/${r.slug}/${r.id}`;
  };

  private convertDate = (r: StudyChapterRelay): StudyChapterRelay => {
    if (typeof r.secondsSinceLastMove !== 'undefined' && !r.lastMoveAt) {
      r.lastMoveAt = Date.now() - r.secondsSinceLastMove * 1000;
    }
    return r;
  };

  private socketHandlers = {
    relayData: (d: RelayData) => {
      if (d.sync) d.sync.log = this.data.sync?.log || [];
      this.data = d;
      this.redraw();
    },
    relaySync: (sync: RelaySync) => {
      this.data.sync = {
        ...sync,
        log: this.data.sync?.log || sync.log,
      };
      this.redraw();
    },
    relayLog: (event: LogEvent) => {
      if (!this.data.sync) return;
      this.data.sync.log.push(event);
      this.data.sync.log = this.data.sync.log.slice(-20);
      this.cooldown = true;
      setTimeout(() => {
        this.cooldown = false;
        this.redraw();
      }, 4500);
      this.redraw();
      if (event.error) {
        if (this.data.sync.log.slice(-2).every(e => e.error)) lichess.sound.play('error');
        console.warn(`relay synchronisation error: ${event.error}`);
      }
    },
  };

  socketHandler = (t: string, d: any): boolean => {
    const handler = (this.socketHandlers as SocketHandlers)[t];
    if (handler && d.id === this.id) {
      handler(d);
      return true;
    }
    return false;
  };
}
