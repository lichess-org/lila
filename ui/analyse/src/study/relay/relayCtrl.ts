import type { RelayData, LogEvent, RelaySync, RelayRound } from './interfaces';
import type { BothClocks, ChapterId, ServerClockMsg } from '@/study/interfaces';
import { type Prop, type Toggle, myUserId, notNull, prop, toggle } from 'lib';
import RelayTeams from './relayTeams';
import RelayPlayers from './relayPlayers';
import type StudyCtrl from '@/study/studyCtrl';
import { VideoPlayer } from './videoPlayer';
import RelayStats from './relayStats';
import { LiveboardPlugin } from './liveboardPlugin';
import { pubsub } from 'lib/pubsub';

export const relayTabs = ['overview', 'boards', 'teams', 'players', 'stats'] as const;
export type RelayTab = (typeof relayTabs)[number];
type StreamInfo = [UserId, { name: string; lang: string }];

export default class RelayCtrl {
  readonly round: RelayRound;
  log: LogEvent[] = [];
  cooldown = false;
  tourShow: Toggle;
  roundSelectShow = toggle(false);
  groupSelectShow = toggle(false);
  tab: Prop<RelayTab>;
  teams?: RelayTeams;
  players: RelayPlayers;
  stats: RelayStats;
  streams: StreamInfo[] = [];
  showStreamerMenu = toggle(false);
  videoPlayer?: VideoPlayer;
  liveboardPlugin?: LiveboardPlugin;

  constructor(
    private readonly study: StudyCtrl,
    public readonly data: RelayData,
  ) {
    this.round = this.data.rounds.find(r => r.id === this.study.data.id)!;
    this.tourShow = toggle((location.pathname.split('/broadcast/')[1].match(/\//g) || []).length < 3, v =>
      v ? study.ctrl.ceval.stop() : study.ctrl.startCeval(),
    );
    if (study.ctrl.opts.chat) {
      const showLiveboard = () => this.tourShow() || !study.multiBoard.showResults();
      this.liveboardPlugin = new LiveboardPlugin(study.ctrl, showLiveboard, study.chapterSelect.get());
      study.ctrl.opts.chat.plugin = this.liveboardPlugin;
    }

    const locationTab = location.hash.replace(/^#(\w+).*$/, '$1') as RelayTab;
    const initialTab = relayTabs.includes(locationTab)
      ? locationTab
      : this.study.chapters.list.looksNew()
        ? 'overview'
        : 'boards';
    this.tab = prop<RelayTab>(initialTab);
    this.teams = data.tour.teamTable
      ? new RelayTeams(this.round, study.multiCloudEval, study.chapterSelect, this.roundPath, this.redraw)
      : undefined;
    this.players = new RelayPlayers(
      data.tour.id,
      () => this.openTab('players'),
      study.ctrl.isEmbed,
      () => study.data.federations,
      () => (study.multiBoard.showResults() ? undefined : this.round.id),
      fideId => data.photos[fideId],
      this.redraw,
    );
    this.stats = new RelayStats(this.round, this.redraw);
    if (data.videoUrls?.[0] || this.isPinnedStreamOngoing())
      this.videoPlayer = new VideoPlayer(
        {
          embed: this.data.videoUrls?.[0] || false,
          redirect: this.data.videoUrls?.[1] || this.data.pinned?.redirect,
          image: this.data.tour.image,
          text: this.data.pinned?.text,
        },
        this.redraw,
      );
    const pinnedName = this.isPinnedStreamOngoing() && data.pinned?.name;
    if (pinnedName) this.streams.push(['ps', { name: pinnedName, lang: '' }]);
    pubsub.on('socket.in.crowd', d => {
      const s = d.streams?.slice() ?? [];
      if (pinnedName) s.unshift(['ps', { name: pinnedName, lang: '' }]);
      if (this.streams.length === s.length && this.streams.every(([id], i) => id === s[i][0])) return;
      this.streams = s;
      this.redraw();
    });
    setInterval(study.ctrl.redraw, 1000);
  }

  redraw = () => {
    this.study.ctrl.redraw();
    this.study.updateHistoryAndAddressBar();
  };

  openTab = (t: RelayTab) => {
    this.players.closePlayer();
    this.tab(t);
    this.tourShow(true);
    this.redraw();
  };

  onChapterChange = (id: ChapterId) => {
    if (this.tourShow()) {
      this.tourShow(false);
    }
    this.liveboardPlugin?.setChapterId(id);
    this.redraw();
  };

  lastMoveAt = (id: ChapterId): number | undefined => this.study.chapters.list.get(id)?.lastMoveAt;

  setSync = (v: boolean) => {
    this.study.ctrl.socket.send('relaySync', v);
    this.redraw();
  };

  loading = () => !this.cooldown && this.data.sync?.ongoing;

  setClockToChapterPreview = (msg: ServerClockMsg, clocks: BothClocks) => {
    const cp = this.study.chapters.list.get(msg.p.chapterId);
    if (cp?.players)
      ['white', 'black'].forEach((color: Color, i) => {
        const clock = clocks[i];
        if (notNull(clock)) cp.players![color].clock = clock;
      });
  };

  fullRoundName = () => `${this.data.tour.name} - ${this.round.name}`;

  tourPath = () => `/broadcast/${this.data.tour.slug}/${this.data.tour.id}`;
  roundPath = (round?: RelayRound) => {
    const r = round || this.round;
    return `/broadcast/${this.data.tour.slug}/${r.slug}/${r.id}`;
  };
  roundUrlWithHash = (round?: RelayRound) => `${this.roundPath(round)}#${this.tab()}`;
  updateAddressBar = (tourUrl: string, roundUrl: string) => {
    const tab = this.tab();
    const tabHash = () => (tab === 'overview' ? '' : tab === 'players' ? this.players.tabHash() : `#${tab}`);
    const url = this.tourShow() ? `${tourUrl}${tabHash()}` : roundUrl;
    // when jumping from a tour tab to another page, remember which tour tab we were on.
    if (!this.tourShow() && location.href.includes('#')) history.pushState({}, '', url);
    else history.replaceState({}, '', url);
  };

  isOfficial = () => !!this.data.tour.tier;

  isStreamer = () => this.streams.some(([id]) => id === myUserId());

  isPinnedStreamOngoing = () => {
    if (!this.data.pinned) return false;
    if (this.round.finished) return false;
    if (Date.now() < this.round.startsAt! - 1000 * 3600) return false;
    return true;
  };

  userClosedTheVideoEmbed() {
    return document.cookie.includes('relayVideo=no');
  }

  onAddNode = () => {
    if (!this.round.ongoing) {
      // we have a move, set the current round as started
      this.round.ongoing = true;
      this.round.startsAt = this.round.startsAt || Date.now();
      this.data.delayedUntil = undefined;
    }
  };

  private socketHandlers = {
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
    },
  };

  socketHandler = (t: string, d: any): boolean => {
    const handler = (this.socketHandlers as SocketHandlers)[t];
    if (handler && d.id === this.study.data.id) {
      handler(d);
      return true;
    }
    return false;
  };
}
