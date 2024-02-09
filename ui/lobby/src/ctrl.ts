import Filter from './filter';
import * as hookRepo from './hookRepo';
import { Hook, LobbyData, LobbyOpts, Mode, Preset, PresetOpts, Seek, Sort, Tab } from './interfaces';
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
  presetOpts: PresetOpts;
  allPresets: Preset[];
  currentPresetId: string;

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
    this.presetOpts = {
      isAnon: !opts.data.me,
      isNewPlayer: !!opts.data.me?.isNewPlayer,
      aiLevel: opts.data.me?.aiLevel && parseInt(opts.data.me.aiLevel),
      rating: opts.data.me?.rating && parseInt(opts.data.me.rating),
      ratingDiff: 300,
    };
    this.filter = new Filter(li.storage.make('lobby.filter'), this);
    this.setup = new Setup(li.storage.make, this);
    this.initAllPresets();

    seekRepo.sort(this);
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
        if (['real_time', 'presets'].includes(this.tab) && !this.data.hooks.length) this.socket.realTimeIn();
      }, 10 * 1000);
    }

    li.pubsub.on('socket.open', () => {
      if (['real_time', 'presets'].includes(this.tab)) {
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
    const highestDefeatedAi = this.presetOpts.aiLevel || 1,
      level = Math.min(Math.max(highestDefeatedAi, 2), 7); // the middle level
    this.allPresets = [
      { lim: 3, byo: 0 },
      { lim: 0, byo: 10 },
      { lim: 5, byo: 0 },
      { lim: 10, byo: 0 },
      { lim: 10, byo: 30 },
      { lim: 30, byo: 0 },
      { lim: 10, ai: level - 1 },
      { lim: 15, ai: level },
      { lim: 20, ai: level + 1 },
      { days: 3 },
      { days: 5 },
    ].map((p, i) => {
      return {
        id: 'pid-' + i,
        lim: p.lim || 0,
        byo: p.byo || 0,
        inc: 0,
        per: 1,
        ai: p.ai,
        days: p.days || 1,
        timeMode: p.days ? 2 : 1,
      };
    });
  };

  setTab = (tab: Tab, store = true) => {
    if (tab !== this.tab) {
      if (tab === 'seeks') xhr.seeks().then(this.setSeeks);
      else if (hookRepo.tabs.includes(tab) && !hookRepo.tabs.includes(this.tab)) this.socket.realTimeIn();
      else if (hookRepo.tabs.includes(this.tab) && !hookRepo.tabs.includes(tab)) {
        this.socket.realTimeOut();
        this.data.hooks = [];
      }
      if (store) this.tab = this.stores.tab.set(tab);
      else this.tab = tab;
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
    const action = hookRepo.action(hook);
    if (action === 'cancel' || variantConfirm(hook.variant || 'standard', this.trans.noarg))
      this.socket.send(action, hook.id);
  };

  clickSeek = (id: string) => {
    const seek = seekRepo.find(this, id);
    if (!seek || this.redirecting) return;
    const action = seekRepo.action(seek, this);
    if (action === 'cancelSeek' || variantConfirm(seek.variant || 'standard', this.trans.noarg))
      this.socket.send(action, seek.id);
  };

  setSeeks = (seeks: Seek[]) => {
    this.data.seeks = seeks;
    seekRepo.sort(this);
    this.redraw();
  };

  clickPreset = preset => {
    xhr.seekFromPreset(preset, this.presetOpts);
    if (preset.ai) {
      this.setRedirecting();
    } else {
      this.currentPresetId = preset.id;
      if (preset.timeMode === 2) this.setTab('seeks', false);
      else this.setTab('real_time', false);
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
      case 'presets':
        this.data.hooks = [];
        this.socket.realTimeIn();
        xhr.seeks().then(this.setSeeks);
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
