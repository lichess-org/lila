import Filter from './filter';
import * as hookRepo from './hookRepo';
import { Hook, LobbyData, LobbyOpts, Mode, Preset, Seek, Sort, Tab } from './interfaces';
import * as seekRepo from './seekRepo';
import Setup from './setup';
import LobbySocket from './socket';
import { Stores, make as makeStores } from './store';
import variantConfirm from './variant';
import * as xhr from './xhr';

const li = window.lishogi;

export default class LobbyController {
  data: LobbyData;
  playban: any;
  isBot: boolean;
  socket: LobbySocket;
  stores: Stores;
  tab: Tab;
  mode: Mode;
  sort: Sort;
  stepHooks: Hook[] = [];
  stepping: boolean = false;
  redirecting: boolean = false;
  trans: Trans;
  filter: Filter;
  setup: Setup;
  allPresets: Preset[];

  private flushHooksTimeout?: number;
  private alreadyWatching: string[] = [];

  constructor(
    readonly opts: LobbyOpts,
    readonly redraw: () => void
  ) {
    this.data = opts.data;
    this.data.hooks = [];
    this.playban = opts.playban;
    this.isBot = opts.data.me && opts.data.me.isBot;
    this.filter = new Filter(li.storage.make('lobby.filter'), this);
    this.setup = new Setup(li.storage.make, this);
    this.initAllPresets();

    hookRepo.initAll(this);
    seekRepo.initAll(this);
    this.socket = new LobbySocket(opts.socketSend, this);

    this.stores = makeStores(this.data.me ? this.data.me.username.toLowerCase() : null);
    (this.tab = this.isBot ? 'now_playing' : this.stores.tab.get()),
      (this.mode = this.stores.mode.get()),
      (this.sort = this.stores.sort.get()),
      (this.trans = opts.trans);

    this.flushHooksSchedule();

    this.startWatching();

    if (this.playban) {
      if (this.playban.remainingSecond < 86400) setTimeout(li.reload, this.playban.remainingSeconds * 1000);
    } else {
      setInterval(() => {
        if (this.tab === 'real_time' && !this.data.hooks.length) this.socket.realTimeIn();
      }, 10 * 1000);
    }

    li.pubsub.on('socket.open', () => {
      if (this.tab === 'real_time') {
        this.data.hooks = [];
        this.socket.realTimeIn();
      }
    });
  }

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

  private flushHooksSchedule = (): number => setTimeout(this.flushHooks, 8000);

  initAllPresets = () => {
    const savedAiLevel = +(this.setup.stores.ai.get()?.level || 2),
      level = Math.min(Math.max(savedAiLevel, 2), 7); // the middle level
    this.allPresets = [
      { lim: 0, byo: 10 },
      { lim: 3, byo: 0 },
      { lim: 5, byo: 10 },
      { lim: 10, byo: 0 },
      { lim: 10, byo: 30 },
      { lim: 15, byo: 30 },
      { lim: 10, ai: level - 1 },
      { lim: 15, ai: level },
      { lim: 20, ai: level + 1 },
      { days: 3 },
      { days: 5 },
    ].map((p, i) => {
      return {
        id: 'pid-' + i,
        lim: p.lim || 1,
        byo: p.byo || 0,
        inc: 0,
        per: 1,
        ai: p.ai,
        days: p.days || 1,
        timeMode: p.days ? 2 : 1,
      };
    });
  };

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

  clickHook = (id: string) => {
    const hook = hookRepo.find(this, id);
    if (!hook || hook.disabled || this.stepping || this.redirecting) return;
    if (hook.action === 'cancel' || variantConfirm(hook.variant, this.trans.noarg))
      this.socket.send(hook.action, hook.id);
  };

  clickSeek = (id: string) => {
    const seek = seekRepo.find(this, id);
    if (!seek || this.redirecting) return;
    if (seek.action === 'cancelSeek' || variantConfirm(seek.variant, this.trans.noarg))
      this.socket.send(seek.action, seek.id);
  };

  setSeeks = (seeks: Seek[]) => {
    this.data.seeks = seeks;
    seekRepo.initAll(this);
    this.redraw();
  };
  clickPreset = preset => {
    xhr.seekFromPreset(preset, !this.data.me);
    if (!preset.ai) {
      if (preset.timeMode === 2) this.setTab('seeks');
      else this.setTab('real_time');
    }
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
        xhr.seeks().then(this.setSeeks);
        break;
    }
  };
}
