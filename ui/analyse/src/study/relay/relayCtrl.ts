import { RelayData, LogEvent, RelaySync, RelayRound, RoundId } from './interfaces';
import { BothClocks, ChapterId, ChapterSelect, Federations, ServerClockMsg } from '../interfaces';
import { StudyMemberCtrl } from '../studyMembers';
import { AnalyseSocketSend } from '../../socket';
import { Prop, Toggle, defined, notNull, prop, toggle } from 'common';
import RelayTeams from './relayTeams';
import RelayLeaderboard from './relayLeaderboard';
import { StudyChapters } from '../studyChapters';
import { MultiCloudEval } from '../multiCloudEval';
import { onWindowResize as videoPlayerOnWindowResize } from './videoPlayerView';
import RelayStats from './relayStats';

export const relayTabs = ['overview', 'boards', 'teams', 'leaderboard', 'stats'] as const;
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
  stats: RelayStats;
  streams: [string, string][] = [];
  showStreamerMenu = toggle(false);

  constructor(
    readonly id: RoundId,
    public data: RelayData,
    readonly send: AnalyseSocketSend,
    readonly redraw: (redrawOnly?: boolean) => void,
    readonly isEmbed: boolean,
    readonly members: StudyMemberCtrl,
    private readonly chapters: StudyChapters,
    private readonly multiCloudEval: MultiCloudEval,
    private readonly federations: () => Federations | undefined,
    chapterSelect: ChapterSelect,
  ) {
    this.tourShow = toggle((location.pathname.split('/broadcast/')[1].match(/\//g) || []).length < 3);
    const locationTab = location.hash.replace(/^#/, '') as RelayTab;
    const initialTab = relayTabs.includes(locationTab)
      ? locationTab
      : this.chapters.looksNew()
      ? 'overview'
      : 'boards';
    this.tab = prop<RelayTab>(initialTab);
    this.teams = data.tour.teamTable
      ? new RelayTeams(id, this.multiCloudEval, chapterSelect, this.roundPath, redraw)
      : undefined;
    this.leaderboard = data.tour.leaderboard
      ? new RelayLeaderboard(data.tour.id, this.federations, redraw)
      : undefined;
    this.stats = new RelayStats(this.currentRound(), redraw);
    setInterval(() => this.redraw(true), 1000);

    const pinned = data.pinnedStream;
    if (data.videoUrls) videoPlayerOnWindowResize(this.redraw);
    if (pinned && this.pinStreamer()) this.streams.push(['', pinned.name]);

    site.pubsub.on('socket.in.crowd', d => {
      const s = (d.streams as [string, string][]) ?? [];
      if (pinned && this.pinStreamer()) s.unshift(['', pinned.name]);
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
  roundUrlWithHash = (round?: RelayRound) => `${this.roundPath(round)}#${this.tab()}`;
  updateAddressBar = (tourUrl: string, roundUrl: string) => {
    const url = this.tourShow() ? `${tourUrl}${this.tab() === 'overview' ? '' : `#${this.tab()}`}` : roundUrl;
    // when jumping from a tour tab to another page, remember which tour tab we were on.
    if (!this.tourShow() && location.href.includes('#')) history.pushState({}, '', url);
    else history.replaceState({}, '', url);
  };

  isStreamer = () => this.streams.some(([id]) => id === document.body.dataset.user);

  pinStreamer = () =>
    defined(this.data.pinnedStream) &&
    !this.currentRound().finished &&
    Date.now() > this.currentRound().startsAt! - 1000 * 3600;

  closeVideoPlayer = () => {
    const url = new URL(location.href);
    url.searchParams.set('embed', 'no');
    url.searchParams.set('nonce', `${Date.now()}`);
    window.location.replace(url);
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
        if (this.data.sync.log.slice(-3).every(e => e.error)) site.sound.play('error');
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
