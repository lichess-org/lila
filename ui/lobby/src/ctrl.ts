import variantConfirm from './variant';
import * as hookRepo from './hookRepo';
import * as seekRepo from './seekRepo';
import { make as makeStores, Stores } from './store';
import * as xhr from './xhr';
import * as poolRangeStorage from './poolRangeStorage';
import { LobbyOpts, LobbyData, Tab, Mode, Sort, Filter, Hook, Seek, Pool, PoolMember } from './interfaces';
import LobbySocket from './socket';

const li = window.lichess;

export default class LobbyController {

  opts: LobbyOpts;
  data: LobbyData;
  playban: any;
  isBot: boolean;
  socket: LobbySocket;
  stores: Stores;
  tab: Tab;
  mode: Mode;
  sort: Sort;
  filterOpen: boolean = false;
  stepHooks: Hook[] = [];
  stepping: boolean = false;
  redirecting: boolean = false;
  poolMember?: PoolMember;
  trans: Trans;
  redraw: () => void;
  pools: Pool[];

  private poolInStorage: LichessStorage;
  private flushHooksTimeout?: number;
  private alreadyWatching: string[] = [];

  constructor(opts: LobbyOpts, redraw: () => void) {
    this.opts = opts;
    this.data = opts.data;
    this.data.hooks = [];
    this.pools = opts.pools;
    this.playban = opts.playban;
    this.isBot = opts.data.me && opts.data.me.isBot;
    this.redraw = redraw;

    hookRepo.initAll(this);
    seekRepo.initAll(this);
    this.socket = new LobbySocket(opts.socketSend, this);

    this.stores = makeStores(this.data.me ? this.data.me.username.toLowerCase() : null);
    this.tab = this.isBot ? 'now_playing' : this.stores.tab.get(),
      this.mode = this.stores.mode.get(),
      this.sort = this.stores.sort.get(),
      this.trans = opts.trans;

    this.poolInStorage = li.storage.make('lobby.pool-in');
    this.poolInStorage.listen(() => { // when another tab joins a pool
      this.leavePool();
      redraw();
    });
    this.flushHooksSchedule();

    this.startWatching();

    if (this.playban) {
      if (this.playban.remainingSecond < 86400) setTimeout(li.reload, this.playban.remainingSeconds * 1000);
    }
    else {
      setInterval(() => {
        if (this.poolMember) this.poolIn();
        else if (this.tab === 'real_time' && !this.data.hooks.length) this.socket.realTimeIn();
      }, 10 * 1000);
      this.onNewOpponent();
    }

    li.pubsub.on('socket.open', () => {
      if (this.tab === 'real_time') {
        this.data.hooks = [];
        this.socket.realTimeIn();
      } else if (this.tab === 'pools' && this.poolMember) this.poolIn();
    });

    window.addEventListener('beforeunload', () => {
      if (this.poolMember) this.socket.poolOut(this.poolMember);
    });
  }

  private doFlushHooks() {
    this.stepHooks = this.data.hooks.slice(0);
    if (this.tab === 'real_time') this.redraw();
  };

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

  private flushHooksSchedule(): number {
    return setTimeout(this.flushHooks, 8000);
  }

  setTab = (tab: Tab) => {
    if (tab !== this.tab) {
      if (tab === 'seeks') xhr.seeks().then(this.setSeeks);
      else if (tab === 'real_time') this.socket.realTimeIn();
      else if (this.tab === 'real_time') {
        this.socket.realTimeOut();
        this.data.hooks = [];
      }
      this.tab = this.stores.tab.set(tab);
    }
    this.filterOpen = false;
  };

  setMode = (mode: Mode) => {
    this.mode = this.stores.mode.set(mode);
    this.filterOpen = false;
  };

  setSort = (sort: Sort) => {
    this.sort = this.stores.sort.set(sort);
  };

  toggleFilter = () => {
    this.filterOpen = !this.filterOpen;
  };

  setFilter = (filter: Filter) => {
    this.data.filter = filter;
    this.flushHooks(true);
    if (this.tab !== 'real_time') this.redraw();
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
    if (!this.data.me) {
      xhr.anonPoolSeek(this.pools.find(function(p) {
        return p.id === id;
      }));
      this.setTab('real_time');
    } else if (this.poolMember && this.poolMember.id === id) this.leavePool();
    else {
      this.enterPool({ id });
      this.redraw();
    }
  };

  enterPool = (member: PoolMember) => {
    poolRangeStorage.set(member.id, member.range);
    this.setTab('pools');
    this.poolMember = member;
    this.poolIn();
  };

  leavePool = () => {
    if (!this.poolMember) return;
    this.socket.poolOut(this.poolMember);
    this.poolMember = undefined;
    this.redraw();
  };

  poolIn = () => {
    if (!this.poolMember) return;
    this.poolInStorage.set(li.StrongSocket.sri);
    this.socket.poolIn(this.poolMember);
  };

  gameActivity = gameId => {
    if (this.data.nowPlaying.find(p => p.gameId === gameId))
      xhr.nowPlaying().then(povs => {
        this.data.nowPlaying = povs;
        this.startWatching();
        this.redraw();
      });
  };

  private startWatching() {
    const newIds = this.data.nowPlaying
      .map(p => p.gameId)
      .filter(id => !this.alreadyWatching.includes(id));
    if (newIds.length) {
      setTimeout(() => this.socket.send("startWatching", newIds.join(' ')), 2000);
      newIds.forEach(id => this.alreadyWatching.push(id));
    }
  };

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
        xhr.seeks().then(this.setSeeks);
        break;
    }
  };

  // after click on round "new opponent" button
  private onNewOpponent() {
    if (location.hash.startsWith('#pool/')) {
      const regex = /^#pool\/(\d+\+\d+)(?:\/(.+))?$/,
        match = regex.exec(location.hash),
        member: any = { id: match![1], blocking: match![2] },
        range = poolRangeStorage.get(member.id);
      if (range) member.range = range;
      if (match) {
        this.setTab('pools');
        this.enterPool(member);
        history.replaceState(null, '', '/');
      }
    }
  }
}
