import { numberFormat } from 'common/number';
import { storedIntProp, Prop } from 'common/storage';
import variantConfirm from './variant';
import * as hookRepo from './hookRepo';
import * as seekRepo from './seekRepo';
import { make as makeStores, Stores } from './store';
import * as xhr from './xhr';
import * as poolRangeStorage from './poolRangeStorage';
import {
  LobbyOpts,
  LobbyData,
  LobbyTab,
  CustomGameTab,
  Mode,
  Sort,
  Hook,
  Seek,
  Pool,
  PoolMember,
  GameType,
  ForceSetupOptions,
  LobbyMe,
} from './interfaces';
import LobbySocket from './socket';
import Filter from './filter';
import SetupController from './setupCtrl';
import disableDarkBoard from './disableDarkBoard';
import { ready as loadDialogPolyfill } from 'common/dialog';

export default class LobbyController {
  data: LobbyData;
  playban: any;
  me?: LobbyMe;
  socket: LobbySocket;
  stores: Stores;
  tab: LobbyTab;
  customGameTab: CustomGameTab;
  mode: Mode;
  sort: Sort;
  stepHooks: Hook[] = [];
  stepping = false;
  redirecting = false;
  feedRev: Prop<number>;
  feedHtml: string;
  poolMember?: PoolMember;
  trans: Trans;
  pools: Pool[];
  filter: Filter;
  setupCtrl: SetupController;

  private poolInStorage: LichessStorage;
  private flushHooksTimeout?: number;
  private alreadyWatching: string[] = [];

  constructor(
    readonly opts: LobbyOpts,
    readonly redraw: () => void,
  ) {
    this.data = opts.data;
    this.data.hooks = [];
    this.me = opts.data.me;
    this.pools = opts.pools;
    this.playban = opts.playban;
    this.filter = new Filter(lichess.storage.make('lobby.filter'), this);
    this.setupCtrl = new SetupController(this);
    this.feedRev = storedIntProp('feed.lastUpdate', opts.data.lastFeedRev);
    this.feedHtml = (opts.appElement.querySelector('.daily-feed__updates') as HTMLElement).innerHTML;
    hookRepo.initAll(this);
    seekRepo.initAll(this);
    this.socket = new LobbySocket(opts.socketSend, this);

    this.stores = makeStores(this.me?.username.toLowerCase());
    if (!this.me?.isBot && this.stores.tab.get() == 'now_playing' && this.data.nbNowPlaying == 0) {
      this.stores.tab.set('pools');
    }
    this.tab = this.me?.isBot ? 'now_playing' : this.stores.tab.get();
    this.customGameTab = this.stores.customGameTab.get();
    this.mode = this.stores.mode.get();
    this.sort = this.stores.sort.get();
    this.trans = opts.trans;

    const locationHash = location.hash.replace('#', '');
    if (['ai', 'friend', 'hook'].includes(locationHash)) {
      let friendUser: string;
      const forceOptions: ForceSetupOptions = {};
      const urlParams = new URLSearchParams(location.search);
      if (locationHash === 'hook') {
        if (urlParams.get('time') === 'realTime') {
          this.tab = 'custom_games';
          this.customGameTab = 'real_time';
          forceOptions.timeMode = 'realTime';
        } else if (urlParams.get('time') === 'correspondence') {
          this.tab = 'custom_games';
          this.customGameTab = 'correspondence';
          forceOptions.timeMode = 'correspondence';
        }
      } else if (urlParams.get('fen')) {
        forceOptions.fen = urlParams.get('fen')!;
        forceOptions.variant = 'fromPosition';
      } else {
        friendUser = urlParams.get('user')!;
      }

      loadDialogPolyfill.then(() => {
        this.setupCtrl.openModal(locationHash as GameType, forceOptions, friendUser);
        redraw();
      });
      history.replaceState(null, '', '/');
    }

    this.poolInStorage = lichess.storage.make('lobby.pool-in');
    this.poolInStorage.listen(_ => {
      // when another tab joins a pool
      this.leavePool();
      redraw();
    });
    this.flushHooksSchedule();

    this.startWatching();

    if (this.playban) {
      if (this.playban.remainingSeconds < 86400)
        setTimeout(lichess.reload, this.playban.remainingSeconds * 1000);
    } else {
      setInterval(() => {
        if (this.poolMember) this.poolIn();
        else if (this.tab === 'custom_games' && this.customGameTab === 'real_time' && !this.data.hooks.length)
          this.socket.realTimeIn();
      }, 10 * 1000);
      this.joinPoolFromLocationHash();
    }

    lichess.pubsub.on('socket.open', () => {
      if (this.tab === 'custom_games' && this.customGameTab === 'real_time') {
        this.data.hooks = [];
        this.socket.realTimeIn();
      } else if (this.tab === 'pools' && this.poolMember) this.poolIn();
      disableDarkBoard();
    });

    window.addEventListener('beforeunload', () => this.leavePool());
  }

  spreadPlayersNumber?: (nb: number) => void;
  spreadGamesNumber?: (nb: number) => void;
  initNumberSpreader = (elm: HTMLAnchorElement, nbSteps: number, initialCount: number) => {
    let previous = initialCount;
    let timeouts: number[] = [];
    const display = (prev: number, cur: number, it: number) => {
      elm.textContent = numberFormat(Math.round((prev * (nbSteps - 1 - it) + cur * (it + 1)) / nbSteps));
    };
    return (nb: number) => {
      if (!nb && nb !== 0) return;
      timeouts.forEach(clearTimeout);
      timeouts = [];
      const interv = Math.abs(lichess.socket.pingInterval() / nbSteps);
      const prev = previous || nb;
      previous = nb;
      for (let i = 0; i < nbSteps; i++)
        timeouts.push(setTimeout(() => display(prev, nb, i), Math.round(i * interv)));
    };
  };

  private doFlushHooks() {
    this.stepHooks = this.data.hooks.slice(0);
    if (this.tab === 'custom_games' && this.customGameTab === 'real_time') this.redraw();
  }

  flushHooks = (now: boolean) => {
    if (this.flushHooksTimeout) clearTimeout(this.flushHooksTimeout);
    if (now) this.doFlushHooks();
    else {
      this.stepping = true;
      if (this.tab === 'custom_games' && this.customGameTab === 'real_time') this.redraw();
      setTimeout(() => {
        this.stepping = false;
        this.doFlushHooks();
      }, 500);
    }
    this.flushHooksTimeout = this.flushHooksSchedule();
  };

  private flushHooksSchedule = (): number => setTimeout(this.flushHooks, 8000);

  setTab = (tab: LobbyTab | CustomGameTab) => {
    if (this.tab === tab || (this.tab === 'custom_games' && this.customGameTab === tab)) return;
    if (this.customGameTab === 'real_time' && this.tab === 'custom_games') {
      this.socket.realTimeOut();
      this.data.hooks = [];
    }
    const newCustomGameTab = tab === 'correspondence' || tab === 'real_time' ? tab : this.customGameTab;
    if (newCustomGameTab === 'correspondence') xhr.seeks().then(this.setSeeks);
    else if (newCustomGameTab === 'real_time') this.socket.realTimeIn();

    this.tab = tab === 'correspondence' || tab === 'real_time' ? 'custom_games' : tab;
    this.customGameTab = newCustomGameTab;
    this.stores.tab.set(this.tab);
    this.stores.customGameTab.set(this.customGameTab);
    this.filter.open = false;
  };

  setMode = (mode: Mode) => {
    this.mode = this.stores.mode.set(mode);
    this.filter.open = false;
  };

  setSort = (sort: Sort) => {
    this.sort = this.stores.sort.set(sort);
  };

  onSetFilter = () => {
    this.flushHooks(true);
    this.redraw();
  };

  clickHook = (id: string) => {
    const hook = hookRepo.find(this, id);
    if (!hook || hook.disabled || this.stepping || this.redirecting) return;
    if (hook.action === 'cancel' || variantConfirm(hook.variant)) this.socket.send(hook.action, hook.id);
  };

  clickSeek = (id: string) => {
    const seek = seekRepo.find(this, id);
    if (!seek || this.redirecting) return;
    if (seek.action === 'cancelSeek' || variantConfirm(seek.variant)) this.socket.send(seek.action, seek.id);
  };

  setSeeks = (seeks: Seek[]) => {
    this.data.seeks = seeks;
    seekRepo.initAll(this);
    this.redraw();
  };

  clickPool = (id: string) => {
    if (!this.me) {
      xhr.anonPoolSeek(this.pools.find(p => p.id == id)!);
      this.setTab('real_time');
    } else if (this.poolMember && this.poolMember.id === id) this.leavePool();
    else this.enterPool({ id });
    this.redraw();
  };

  enterPool = (member: PoolMember) => {
    poolRangeStorage.set(this.me, member);
    this.setTab('pools');
    this.poolMember = member;
    this.poolIn();
  };

  leavePool = () => {
    if (!this.poolMember) return;
    this.socket.poolOut(this.poolMember);
    this.poolMember = undefined;
  };

  poolIn = () => {
    if (!this.poolMember) return;
    this.poolInStorage.fire();
    this.socket.poolIn(this.poolMember);
  };

  hasOngoingRealTimeGame = () =>
    !!this.data.nowPlaying.find(nowPlaying => nowPlaying.isMyTurn && nowPlaying.speed !== 'correspondence');

  gameActivity = (gameId: string) => {
    if (this.data.nowPlaying.find(p => p.gameId === gameId))
      xhr.nowPlaying().then(povs => {
        this.data.nowPlaying = povs;
        this.startWatching();
        this.redraw();
      });
  };

  private startWatching() {
    const newIds = this.data.nowPlaying.map(p => p.gameId).filter(id => !this.alreadyWatching.includes(id));
    if (newIds.length) {
      setTimeout(() => this.socket.send('startWatching', newIds.join(' ')), 2000);
      newIds.forEach(id => this.alreadyWatching.push(id));
    }
  }

  setRedirecting = () => {
    this.redirecting = true;
    setTimeout(() => {
      this.redirecting = false;
      this.redraw();
    }, 4000);
    this.redraw();
  };

  awake = () => {
    if (this.tab !== 'custom_games') return;
    if (this.customGameTab === 'correspondence') xhr.seeks().then(this.setSeeks);
    else if (this.customGameTab === 'real_time') {
      this.data.hooks = [];
      this.socket.realTimeIn();
    }
  };

  // after click on round "new opponent" button
  // also handles onboardink link for anon users
  private joinPoolFromLocationHash = () => {
    if (location.hash.startsWith('#pool/')) {
      const regex = /^#pool\/(\d+\+\d+)(?:\/(.+))?$/,
        match = regex.exec(location.hash),
        member: any = { id: match![1], blocking: match![2] },
        range = poolRangeStorage.get(this.me, member.id);
      if (range) member.range = range;
      if (match) {
        this.setTab('pools');
        if (this.me) this.enterPool(member);
        else setTimeout(() => this.clickPool(member.id), 1500);
        history.replaceState(null, '', '/');
      }
    }
  };

  get feedUpdates(): boolean {
    return this.data.lastFeedRev !== this.feedRev();
  }

  set feedUpdates(v: false) {
    if (v === false) this.feedRev(this.data.lastFeedRev);
  }

  updateFeed(rev: number) {
    if (rev === this.data.lastFeedRev) return;
    this.data.lastFeedRev = rev;
    this.redraw();
  }
}
