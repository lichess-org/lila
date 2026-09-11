import { numberFormat } from 'lib/i18n';
import * as poolRangeStorage from 'lib/poolRangeStorage';
import { pubsub } from 'lib/pubsub';
import { wsPingInterval } from 'lib/socket';
import { storage, type LichessStorage } from 'lib/storage';

import Filter from './filter';
import * as hookRepo from './hookRepo';
import type { LobbyOpts, LobbyData, Tab, Mode, Sort, Hook, PoolMember, LobbyMe } from './interfaces';
import * as seekRepo from './seekRepo';
import SetupController from './setupCtrl';
import { ShortcutsCtrl, pools } from './shortcutsCtrl';
import LobbySocket from './socket';
import { make as makeStores, type Stores } from './store';
import { parseUrlParams } from './urlParams';
import variantConfirm from './variant';
import * as xhr from './xhr';

export default class LobbyController {
  data: LobbyData;
  playban: any;
  me?: LobbyMe;
  socket: LobbySocket;
  stores: Stores;
  tab: Tab;
  mode: Mode;
  sort: Sort;
  stepHooks: Hook[] = [];
  stepping = false;
  redirecting = false;
  poolMember?: PoolMember;
  filter: Filter;
  setupCtrl: SetupController;
  shortcutsCtrl: ShortcutsCtrl;

  private readonly poolInStorage: LichessStorage;
  private flushHooksTimeout?: number;
  private readonly alreadyWatching: string[] = [];

  constructor(
    readonly opts: LobbyOpts,
    readonly redraw: () => void,
  ) {
    this.data = {
      ...opts.data,
      hooks: [],
      seeks: [],
    };
    this.me = opts.data.me;
    this.shortcutsCtrl = new ShortcutsCtrl(this);
    this.playban = opts.playban;
    this.filter = new Filter(storage.make('lobby.filter'), this);
    this.setupCtrl = new SetupController(this);
    hookRepo.initAll(this);
    seekRepo.initAll(this);
    this.socket = new LobbySocket(opts.socketSend, this);

    this.stores = makeStores(this.me?.username.toLowerCase());
    if (this.me?.isBot) this.tab = 'now_playing';
    else {
      if (this.stores.tab.get() === 'now_playing' && this.data.nbNowPlaying === 0)
        this.stores.tab.set('shortcuts');
      else if (this.hasOngoingRealTimeGame(false)) this.stores.tab.set('now_playing');
      this.tab = this.stores.tab.get();
    }
    this.mode = this.stores.mode.get();
    this.sort = this.me ? this.stores.sort.get() : 'time';

    const urlParams = parseUrlParams(location);
    if (urlParams) {
      const { gameType, forceOptions, friendUser } = urlParams;
      if (gameType === 'hook') {
        if (forceOptions.timeMode === 'realTime') this.tab = 'real_time';
        else if (forceOptions.timeMode) this.tab = 'seeks';
      }

      pubsub.after('polyfill.dialog').then(() => {
        this.setupCtrl.openModal(gameType, forceOptions, friendUser);
        redraw();
      });
      history.replaceState(null, '', '/');
    }

    this.poolInStorage = storage.make('lobby.pool-in');
    this.poolInStorage.listen(_ => {
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
        else if (this.tab === 'real_time' && !this.data.hooks.length) this.socket.realTimeIn();
      }, 10 * 1000);
      this.joinPoolFromLocationHash();
    }

    pubsub.on('socket.open', () => {
      if (this.tab === 'real_time') {
        this.data.hooks = [];
        this.socket.realTimeIn();
      } else if (this.tab === 'shortcuts' && this.poolMember) this.poolIn();
      else if (this.tab === 'seeks') this.fetchSeeks();
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
      const interv = Math.abs(wsPingInterval() / nbSteps);
      const prev = previous || nb;
      previous = nb;
      for (let i = 0; i < nbSteps; i++)
        timeouts.push(setTimeout(() => display(prev, nb, i), Math.round(i * interv)));
    };
  };

  private doFlushHooks() {
    this.stepHooks = this.data.hooks.slice(0);
    if (this.tab === 'real_time') this.redraw();
  }

  flushHooks = (now: boolean) => {
    if (this.flushHooksTimeout) clearTimeout(this.flushHooksTimeout);
    if (now) this.doFlushHooks();
    else {
      this.stepping = true;
      if (this.tab === 'real_time') this.redraw();
      setTimeout(() => {
        this.stepping = false;
        this.doFlushHooks();
      }, 500);
    }
    this.flushHooksTimeout = this.flushHooksSchedule();
  };

  private readonly flushHooksSchedule = () => setTimeout(this.flushHooks, 8000);

  setTab = (tab: Tab) => {
    if (tab !== this.tab) {
      if (tab === 'seeks') this.fetchSeeks();
      else if (tab === 'real_time') this.socket.realTimeIn();
      else if (this.tab === 'real_time') {
        this.socket.realTimeOut();
        this.data.hooks = [];
      }
      this.tab = this.stores.tab.set(tab);
      this.redraw();
    }
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
    if (this.tab !== 'real_time') this.redraw();
  };

  clickHook = async (id: string) => {
    const hook = hookRepo.find(this, id);
    if (!hook || hook.disabled || this.stepping || this.redirecting) return;
    if (hook.action === 'cancel' || (await variantConfirm(hook.variant)))
      this.socket.send(hook.action, hook.id);
  };

  clickSeek = async (id: string) => {
    const seek = seekRepo.find(this, id);
    if (!seek || this.redirecting) return;
    if (seek.action === 'cancelSeek' || (await variantConfirm(seek.variant?.key)))
      this.socket.send(seek.action, seek.id);
  };

  fetchSeeks = async () => {
    this.data.seeks = await xhr.seeks();
    seekRepo.initAll(this);
    this.redraw();
  };

  clickPool = (id: string) => {
    if (!this.me) {
      xhr.anonPoolSeek(pools.find(p => p.id === id)!);
      this.setTab('real_time');
    } else if (this.poolMember?.id === id) this.leavePool();
    else this.enterPool({ id });
    this.redraw();
  };

  enterPool = (member: PoolMember) => {
    poolRangeStorage.set(this.me?.username, member.id, member.range);
    this.setTab('shortcuts');
    this.poolMember = member;
    this.poolIn();
    site.mousetrap.bind(
      'esc',
      () => {
        this.leavePool();
        this.redraw();
      },
      undefined,
      false,
    );
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

  hasOngoingRealTimeGame = (requireTurn: boolean) =>
    this.data.nowPlaying.some(
      nowPlaying =>
        nowPlaying.speed !== 'correspondence' &&
        (nowPlaying.isMyTurn || !requireTurn) &&
        !nowPlaying.opponent.ai,
    );

  gameActivity = (gameId: string) => {
    if (this.data.nowPlaying.some(p => p.gameId === gameId))
      xhr.nowPlaying().then(res => {
        this.data.nowPlaying = res.nowPlaying;
        this.data.nbMyTurn = res.nbMyTurn;
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
    switch (this.tab) {
      case 'real_time':
        this.data.hooks = [];
        this.socket.realTimeIn();
        break;
      case 'seeks':
        this.fetchSeeks();
        break;
    }
  };

  // after click on round "new opponent" button
  // also handles onboardink link for anon users
  private readonly joinPoolFromLocationHash = () => {
    if (location.hash.startsWith('#pool/')) {
      const regex = /^#pool\/(\d+\+\d+)(?:\/(.+))?$/,
        match = regex.exec(location.hash),
        member: PoolMember = { id: match![1], blocking: match![2] },
        range = poolRangeStorage.get(this.me?.username, member.id);
      if (range) member.range = range;
      if (match) {
        this.setTab('shortcuts');
        if (this.me) this.enterPool(member);
        else setTimeout(() => this.clickPool(member.id), 1500);
        history.replaceState(null, '', '/');
      }
    }
  };
}
