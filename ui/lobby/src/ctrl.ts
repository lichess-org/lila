import { numberFormat } from 'common/number';
import { trans } from 'common/i18n';
import variantConfirm from './variant';
import * as hookRepo from './hookRepo';
import * as seekRepo from './seekRepo';
import { make as makeStores, Stores } from './store';
import * as xhr from './xhr';
import * as poolRangeStorage from './poolRangeStorage';
import { LobbyOpts, LobbyData, Tab, Mode, Sort, Hook, Seek, Pool, PoolMember, LobbyMe } from './interfaces';
import { makeSocket, LobbySocket } from './socket';
import Filter from './filter';
import SetupController from './setupCtrl';
import { TabCtrl } from './tabCtrl';
import { storage, type LichessStorage } from 'common/storage';
import { pubsub } from 'common/pubsub';
import { pools } from './constants';

export default class LobbyController {
  data: LobbyData;
  playban: any;
  me?: LobbyMe;
  socket: LobbySocket;
  stores: Stores;
  tab: TabCtrl;
  mode: Mode;
  sort: Sort;
  stepHooks: Hook[] = [];
  stepping = false;
  redirecting = false;
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
    this.pools = pools;
    this.playban = opts.playban;
    this.filter = new Filter(storage.make('lobby.filter'), this);
    this.setupCtrl = new SetupController(this);

    hookRepo.initAll(this);
    seekRepo.initAll(this);
    this.socket = makeSocket(this);

    this.stores = makeStores(this.me?.username.toLowerCase());
    this.tab = new TabCtrl(this);
    this.mode = this.stores.mode.get();
    this.sort = this.stores.sort.get();
    this.trans = trans(opts.i18n);

    this.poolInStorage = storage.make('lobby.pool-in');
    this.poolInStorage.listen(() => {
      // when another tab joins a pool
      this.leavePool();
      redraw();
    });
    this.flushHooksSchedule();
    this.startWatching();

    if (this.playban) {
      if (this.playban.remainingSeconds < 86400)
        setTimeout(site.reload, this.playban.remainingSeconds * 1000);
    } else {
      setInterval(() => {
        if (this.poolMember) this.poolIn();
        else if (this.tab.showingHooks && !this.data.hooks.length) this.socket.realTimeIn();
      }, 10 * 1000);
      this.joinPoolFromLocationHash();
    }
    pubsub.on('socket.open', () => {
      if (this.tab.showingHooks) {
        this.data.hooks = [];
        this.socket.realTimeIn();
      } else if (this.tab.active === 'quick' && this.poolMember) this.poolIn();
    });
    pubsub.after('socket.hasConnected').then(() => {
      const gameId = new URLSearchParams(location.search).get('hook_like');
      if (!gameId) return;
      const { ratingMin, ratingMax } = this.setupCtrl.makeSetupStore('hook')();
      xhr.text(
        xhr.url(`/setup/hook/${site.sri}/like/${gameId}`, { deltaMin: ratingMin, deltaMax: ratingMax }),
        { method: 'post' },
      );
      this.setTab('lobby');
      this.redraw();
      history.replaceState(null, '', '/');
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
      const interv = Math.abs(site.socket.pingInterval() / nbSteps);
      const prev = previous || nb;
      previous = nb;
      for (let i = 0; i < nbSteps; i++)
        timeouts.push(setTimeout(() => display(prev, nb, i), Math.round(i * interv)));
    };
  };

  private doFlushHooks() {
    this.stepHooks = this.data.hooks.slice(0);
    if (this.tab.primary === 'lobby') this.redraw();
  }

  flushHooks = (now: boolean) => {
    if (this.flushHooksTimeout) clearTimeout(this.flushHooksTimeout);
    if (now) this.doFlushHooks();
    else {
      this.stepping = true;
      if (this.tab.primary === 'lobby') this.redraw();
      setTimeout(() => {
        this.stepping = false;
        this.doFlushHooks();
      }, 500);
    }
    this.flushHooksTimeout = this.flushHooksSchedule();
  };

  private flushHooksSchedule = (): number => setTimeout(this.flushHooks, 8000);

  setTab = (tab: Tab) => {
    this.tab.setTab(tab);
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
      this.setTab('realtime');
    } else if (this.poolMember && this.poolMember.id === id) this.leavePool();
    else this.enterPool({ id });
    this.redraw();
  };

  enterPool = (member: PoolMember) => {
    poolRangeStorage.set(this.me, member);
    this.setTab('quick');
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
    if (this.tab.active === 'correspondence') xhr.seeks().then(this.setSeeks);
    else if (this.tab.showingHooks) {
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
        this.setTab('quick');
        if (this.me) this.enterPool(member);
        else setTimeout(() => this.clickPool(member.id), 1500);
        history.replaceState(null, '', '/');
      }
    }
  };
}
