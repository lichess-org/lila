import { RelayData, LogEvent, RelaySync, RelayRound, RoundId } from './interfaces';
import { BothClocks, ChapterId, Federations, ServerClockMsg } from '../interfaces';
import { StudyMemberCtrl } from '../studyMembers';
import { AnalyseSocketSend } from '../../socket';
import { Prop, Toggle, notNull, prop, toggle } from 'common';
import RelayTeams from './relayTeams';
import RelayLeaderboard from './relayLeaderboard';
import { StudyChapters } from '../studyChapters';
import { MultiCloudEval } from '../multiCloudEval';
import { addResizeListener } from './relayView';

export const relayTabs = ['overview', 'boards', 'teams', 'leaderboard'] as const;
export type RelayTab = (typeof relayTabs)[number];

export default class RelayCtrl {
  log: LogEvent[] = [];
  cooldown = false;
  tourShow: Toggle;
  roundSelectShow = toggle(false);
  groupSelectShow = toggle(false);
  tab: Prop<RelayTab>;
  teams?: RelayTeams;
  leaderboard?: RelayLeaderboard;
  streams: [string, string][] = [];
  showStreamerMenu = toggle(false);

  constructor(
    readonly id: RoundId,
    public data: RelayData,
    readonly send: AnalyseSocketSend,
    readonly redraw: (redrawOnly?: boolean) => void,
    readonly members: StudyMemberCtrl,
    private readonly chapters: StudyChapters,
    private readonly multiCloudEval: MultiCloudEval,
    private readonly federations: () => Federations | undefined,
    setChapter: (id: ChapterId | number) => boolean,
  ) {
    this.tourShow = toggle((location.pathname.match(/\//g) || []).length < 5);
    const locationTab = location.hash.replace(/^#/, '') as RelayTab;
    const initialTab = relayTabs.includes(locationTab)
      ? locationTab
      : this.chapters.looksNew()
      ? 'overview'
      : 'boards';
    this.tab = prop<RelayTab>(initialTab);
    this.teams = data.tour.teamTable
      ? new RelayTeams(id, this.multiCloudEval, setChapter, this.roundPath, redraw)
      : undefined;
    this.leaderboard = data.tour.leaderboard
      ? new RelayLeaderboard(data.tour.id, this.federations, redraw)
      : undefined;
    setInterval(() => this.redraw(true), 1000);

    const pinned = data.pinned;
    addResizeListener(this.redraw);
    if (pinned && this.pinStreamer()) this.streams.push([pinned.userId, pinned.name]);

    site.pubsub.on('socket.in.crowd', d => {
      const s = (d.streams as [string, string][]) ?? [];
      if (pinned && this.pinStreamer() && !s.find(x => x[0] === pinned.userId))
        s.unshift([pinned.userId, pinned.name]);
      if (!s) return;
      if (this.streams.length === s.length && this.streams.every(([id], i) => id === s[i][0])) return;
      this.streams = s;
      this.redraw();
    });
  }

  openTab = (t: RelayTab) => {
    this.tab(t);
    this.tourShow(true);
    this.redraw();
  };

  lastMoveAt = (id: ChapterId): number | undefined => this.chapters.get(id)?.lastMoveAt;

  setSync = (v: boolean) => {
    this.send('relaySync', v);
    this.redraw();
  };

  loading = () => !this.cooldown && this.data.sync?.ongoing;

  setClockToChapterPreview = (msg: ServerClockMsg, clocks: BothClocks) => {
    const cp = this.chapters.get(msg.p.chapterId);
    if (cp?.players)
      ['white', 'black'].forEach((color: Color, i) => {
        const clock = clocks[i];
        if (notNull(clock)) cp.players![color].clock = clock;
      });
  };

  roundById = (id: string) => this.data.rounds.find(r => r.id == id);
  currentRound = () => this.roundById(this.id)!;

  fullRoundName = () => `${this.data.tour.name} - ${this.currentRound().name}`;

  tourPath = () => `/broadcast/${this.data.tour.slug}/${this.data.tour.id}`;
  roundPath = (round?: RelayRound) => {
    const r = round || this.currentRound();
    return `/broadcast/${this.data.tour.slug}/${r.slug}/${r.id}`;
  };

  updateAddressBar = (tourUrl: string, roundUrl: string) => {
    const url = this.tourShow() ? `${tourUrl}${this.tab() === 'overview' ? '' : `#${this.tab()}`}` : roundUrl;
    // when jumping from a tour tab to another page, remember which tour tab we were on.
    if (!this.tourShow() && location.href.includes('#')) history.pushState({}, '', url);
    else history.replaceState({}, '', url);
  };

  isStreamer = () => this.streams.some(([id]) => id === document.body.dataset.user);

  pinStreamer = () =>
    !this.currentRound().finished &&
    Date.now() > this.currentRound().startsAt! - 1000 * 3600 &&
    this.data.pinned != undefined;

  hidePinnedImageAndRemember = () => {
    site.storage.set(`relay.hide-image.${this.id}`, 'true');
    const url = new URL(location.href);
    url.searchParams.delete('embed');
    url.searchParams.set('nonce', `${Date.now()}`);
    window.location.replace(url);
  };

  allowPinnedImageOnUniboards = () => site.storage.get(`relay.hide-image.${this.id}`) !== 'true';

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
        if (this.data.sync.log.slice(-2).every(e => e.error)) site.sound.play('error');
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
