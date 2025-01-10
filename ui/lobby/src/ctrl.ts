import { loadChushogiPieceSprite, loadKyotoshogiPieceSprite } from 'common/assets';
import throttle from 'common/throttle';
import Filter from './filter';
import * as hookRepo from './hook-repo';
import type {
  Hook,
  LobbyData,
  LobbyOpts,
  Mode,
  Preset,
  PresetOpts,
  RatingsRecord,
  Seek,
  Sort,
  Tab,
} from './interfaces';
import * as seekRepo from './seek-repo';
import SetupCtrl from './setup/ctrl';
import LobbySocket from './socket';
import { type Stores, make as makeStores } from './store';
import { action } from './util';
import variantConfirm from './variant';
import * as xhr from './xhr';

const li = window.lishogi;

export default class LobbyController {
  data: LobbyData;
  playban: any;
  isAnon: boolean;
  isBot: boolean;
  socket: LobbySocket;
  stores: Stores;
  tab: Tab;
  mode: Mode;
  sort: Sort;
  stepHooks: Hook[] = [];
  stepping = false;
  reloadSeeks = false;
  redirecting = false;
  filter: Filter;
  ratings: RatingsRecord | undefined;
  presetOpts: PresetOpts;
  allPresets: Preset[];
  currentPresetId: string | undefined; // real time only
  setupCtrl: SetupCtrl;

  private flushHooksTimeout?: number;
  private alreadyWatching: string[] = [];

  constructor(
    readonly opts: LobbyOpts,
    readonly redraw: () => void,
  ) {
    this.data = opts.data;
    this.data.hooks = [];
    this.playban = opts.playban;
    this.isBot = opts.data.me?.isBot;
    this.isAnon = !opts.data.me;
    this.ratings = opts.data.me?.ratings;
    this.presetOpts = {
      isAnon: this.isAnon,
      isNewPlayer: !!opts.data.me?.isNewPlayer,
      aiLevel: opts.data.me?.aiLevel && Number.parseInt(opts.data.me.aiLevel),
      ratings: opts.data.me?.ratings,
      ratingDiff: 300,
    };
    this.filter = new Filter(li.storage.make('lobby.filter2'), this);
    this.setupCtrl = new SetupCtrl(this);
    this.initAllPresets();

    this.socket = new LobbySocket(opts.socketSend, this);

    this.stores = makeStores(this.data.me ? this.data.me.username.toLowerCase() : null);
    (this.tab = this.isBot ? 'now_playing' : this.stores.tab.get()),
      (this.mode = this.stores.mode.get()),
      (this.sort = this.stores.sort.get());

    this.flushHooksSchedule();

    this.updatePieceThemes();
    this.startWatching();

    if (this.playban) {
      if (this.playban.remainingSecond < 86400)
        setTimeout(li.reload, this.playban.remainingSeconds * 1000);
    } else {
      setInterval(() => {
        if (hookRepo.tabs.includes(this.tab) && !this.data.hooks.length) this.socket.realTimeIn();
      }, 15 * 1000);
    }

    li.pubsub.on('socket.open', () => {
      if (hookRepo.tabs.includes(this.tab)) {
        this.data.hooks = [];
        this.socket.realTimeIn();
      }
    });
  }

  private doFlushHooks() {
    this.stepHooks = this.data.hooks.slice(0);
    if (this.tab === 'real_time') this.redraw();
  }

  flushHooks = (now: boolean): void => {
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

  initAllPresets = (): void => {
    const highestDefeatedAi = this.presetOpts.aiLevel || 1,
      level = Math.min(Math.max(highestDefeatedAi, 2), 7); // the middle level
    this.allPresets = [
      { lim: 3, byo: 0 },
      { lim: 0, byo: 10 },
      { lim: 5, byo: 0 },
      { lim: 10, byo: 0 },
      { lim: 10, byo: 30 },
      { lim: 15, byo: 60 },
      { lim: 30, ai: level - 1 },
      { lim: 30, ai: level },
      { lim: 30, ai: level + 1 },
      { days: 3 },
      { days: 5 },
    ].map((p, i) => {
      return {
        id: `pid-${i}`,
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

  setTab = (tab: Tab, store = true): void => {
    if (tab !== this.tab) {
      if (tab === 'seeks') this.seeksNow();
      else if (hookRepo.tabs.includes(tab) && !hookRepo.tabs.includes(this.tab))
        this.socket.realTimeIn();
      else if (hookRepo.tabs.includes(this.tab) && !hookRepo.tabs.includes(tab)) {
        this.socket.realTimeOut();
        this.data.hooks = [];
      }
      if (tab === 'presets' && this.reloadSeeks) this.seeksEventually();
      if (store) this.tab = this.stores.tab.set(tab);
      else this.tab = tab;
    }
    this.filter.open = false;
  };

  setMode = (mode: Mode): void => {
    this.mode = this.stores.mode.set(mode);
    this.filter.open = false;
  };

  setSort = (sort: Extract<'rating' | 'time', Sort>): void => {
    if (sort === this.sort) this.sort = this.stores.sort.set(`${sort}-reverse`);
    else this.sort = this.stores.sort.set(sort);
  };

  onSetFilter = (): void => {
    this.flushHooks(true);
    if (this.tab !== 'real_time') this.redraw();
  };

  clickHook = (id: string): void => {
    const hook = hookRepo.find(this, id);
    if (!hook || hook.disabled || this.stepping || this.redirecting) return;
    const act = action(hook);
    if (act === 'cancel' || variantConfirm(hook.variant || 'standard'))
      this.socket.send(act, hook.id);
  };

  clickSeek = (id: string): void => {
    const seek = seekRepo.find(this, id);
    if (!seek || this.redirecting) return;
    const act = action(seek);
    if (this.isAnon) window.location.href = '/signup';
    else if (act === 'cancel' || variantConfirm(seek.variant || 'standard'))
      this.socket.send(`${act}Seek`, seek.id);
  };

  seeksNow: () => void = throttle(100, () => xhr.seeks().then(this.setSeeks));
  seeksEventually: () => void = throttle(7000, () => xhr.seeks().then(this.setSeeks));

  setSeeks = (seeks: Seek[]): void => {
    this.reloadSeeks = false;
    this.data.seeks = seeks;
    this.redraw();
  };

  clickPreset = (preset: Preset): void => {
    xhr.seekFromPreset(preset, this.presetOpts);
    if (preset.ai) {
      this.setRedirecting();
    } else {
      if (preset.timeMode === 2) this.setTab('seeks', false);
      else {
        this.currentPresetId = preset.id;
        this.setTab('real_time', false);
      }
    }
  };

  gameActivity = (gameId: string): void => {
    if (this.data.nowPlaying.find(p => p.gameId === gameId))
      xhr.nowPlaying().then(povs => {
        this.data.nowPlaying = povs;
        this.updatePieceThemes();
        this.startWatching();
        this.redraw();
      });
  };

  private updatePieceThemes() {
    if (this.data.nowPlaying.some(pov => pov.variant.key === 'chushogi')) loadChushogiPieceSprite();
    if (this.data.nowPlaying.some(pov => pov.variant.key === 'kyotoshogi'))
      loadKyotoshogiPieceSprite();
  }

  private startWatching() {
    const newIds = this.data.nowPlaying
      .map(p => p.gameId)
      .filter(id => !this.alreadyWatching.includes(id));
    if (newIds.length) {
      setTimeout(() => this.socket.send('startWatching', newIds.join(' ')), 2000);
      newIds.forEach(id => this.alreadyWatching.push(id));
    }
  }

  setRedirecting = (): void => {
    this.redirecting = true;
    setTimeout(() => {
      this.redirecting = false;
      this.redraw();
    }, 4000);
    this.redraw();
  };

  awake = (): void => {
    switch (this.tab) {
      case 'presets':
        this.data.hooks = [];
        this.socket.realTimeIn();
        this.seeksNow();
        break;
      case 'real_time':
        this.data.hooks = [];
        this.socket.realTimeIn();
        break;
      case 'seeks':
        this.seeksNow();
        break;
    }
  };
}
