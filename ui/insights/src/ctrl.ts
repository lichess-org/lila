import { debounce } from 'common/timings';
import { defaultFilter, filterOptions } from './filter';
import {
  type InsightData,
  type InsightFilter,
  type InsightOpts,
  type Redraw,
  type Tab,
  tabs,
  variants,
} from './types';
import { idFromSpeed, idFromVariant } from './util';

export default class InsightCtrl {
  userId: string;
  username: string;
  usernameHash: string;
  endpoint: string;
  isBot: boolean;

  isError: boolean;

  activeTab: Tab;
  mostPlayedMovesColor: Color;

  filterToggle: boolean; // mobile view

  data: InsightData;
  filter: InsightFilter;

  pref: any;

  constructor(
    opts: InsightOpts,
    readonly redraw: Redraw,
  ) {
    this.username = opts.username;
    this.userId = this.username.toLowerCase();
    this.usernameHash = opts.usernameHash;
    this.endpoint = opts.endpoint;
    this.isBot = opts.isBot;
    this.pref = opts.pref;

    const params = new URL(self.location.href).searchParams;
    this.processFilter(params);

    this.isError = false;

    if (this.filter.color === 'gote') this.mostPlayedMovesColor = 'gote';
    else this.mostPlayedMovesColor = 'sente';

    const path = window.location.pathname.split('/').filter(c => c !== '');
    if (path[path.length - 1] && tabs.includes(path[path.length - 1] as Tab)) {
      this.activeTab = path[path.length - 1] as Tab;
    } else {
      this.activeTab = 'outcomes';
    }
    this.updateUrl();

    this.resetData();

    this.fetchData(this.activeTab);
  }

  updateUrl(): void {
    const q = this.queryString(this.activeTab, false);
    window.history.replaceState(
      '',
      '',
      `/insights/${this.userId}/${this.activeTab}${q ? `?${q}` : ''}`,
    );
  }

  changeTab(tab: Tab): void {
    this.activeTab = tab;
    this.updateUrl();
    if (!this.data[tab]) this.fetchData(tab);
    this.redraw();
  }

  processFilter(params: URLSearchParams): void {
    const df = defaultFilter(this.isBot);
    const flt: Partial<InsightFilter> = this.filter || df;

    const keys = Object.keys(flt) as (keyof InsightFilter)[];
    for (const key of keys) {
      let val: any = params.get(key);
      if (val) {
        if (key === 'since') val = Number.parseInt(val);
        else if (key === 'variant') val = variants[(Number.parseInt(val) || 1) - 1];
        else if (key === 'speeds')
          val = val
            .split('')
            .map((n: string) => Number.parseInt(n))
            .filter((n: number) => !Number.isNaN(n));
        val = val || flt[key] || df[key];
        if (key !== 'custom') {
          const opt = filterOptions(key);
          if (opt.includes(val)) flt[key] = val;
        }
      }
    }

    const customType = params.get('customType') === 'moves' ? 'moves' : 'game';
    flt.custom = {
      type: customType,
      x: params.get('xKey') || (customType === 'game' ? 'color' : 'roles'),
      y: params.get('yKey') || (customType === 'game' ? 'nbOfMovesAndDrops' : 'nbOfMovesAndDrops'),
    };

    this.filter = flt as InsightFilter;
  }

  fetchData(tab: Tab): void {
    this.isError = false;
    const queryString = this.queryString(tab, true);
    const path = `${this.endpoint}/${tab}${queryString ? `?${queryString}` : ''}`;
    fetch(path, {
      headers: {
        'Content-Type': 'application/json',
        Authorization: this.usernameHash,
      },
    })
      .then(response => {
        return response.json();
      })
      .then(data => {
        console.info(data);
        this.data[tab] = data;
        this.redraw();
      })
      .catch(err => {
        console.error(err);
        this.isError = true;
        this.redraw();
      });
  }

  queryString(tab: Tab, forApi: boolean): string {
    const params: Record<string, string> = {};
    const df = defaultFilter(false);
    if (forApi) {
      params.u = this.userId;
      params.tmz =
        window.Intl && Intl.DateTimeFormat
          ? Intl.DateTimeFormat().resolvedOptions().timeZone
          : 'UTC';
    }

    if (this.filter.since !== df.since) params.since = this.filter.since;
    if (this.filter.variant !== df.variant)
      params.variant = idFromVariant(this.filter.variant).toString();
    if (this.filter.color !== df.color) params.color = this.filter.color;
    if (this.filter.rated !== df.rated) params.rated = this.filter.rated;
    if (this.filter.computer !== df.computer) params.computer = this.filter.computer;
    if (this.filter.speeds.length < df.speeds.length)
      params.speeds = this.filter.speeds.map((s: Speed) => idFromSpeed(s)).join('');

    if (tab === 'custom') {
      params.customType = this.filter.custom.type;
      params.xKey = this.filter.custom.x;
      params.yKey = this.filter.custom.y;
    }

    return new URLSearchParams(params).toString();
  }

  updateCustom(key: 'x' | 'y' | 'type', value: string): void {
    if (key === 'type') {
      this.filter.custom.type = value as any;
      this.filter.custom.x = value === 'game' ? 'color' : 'roles';
      this.filter.custom.y = value === 'game' ? 'nbOfMovesAndDrops' : 'nbOfMovesAndDrops';
    } else this.filter.custom[key] = value;

    this.updateUrl();
    this.data.custom = undefined;
    this.redraw();
    this.fetchData('custom');
  }

  updateFilter(flt: Partial<InsightFilter>, toDebounce = false): void {
    if (flt.color && flt.color !== 'both') this.mostPlayedMovesColor = flt.color;

    Object.assign(this.filter, flt);
    this.updateUrl();
    this.resetData();
    this.redraw();
    if (toDebounce) debounce(() => this.fetchData(this.activeTab), 1500)();
    else this.fetchData(this.activeTab);
  }

  resetData(): void {
    this.data = {
      outcomes: undefined,
      moves: undefined,
      times: undefined,
      analysis: undefined,
      opponents: undefined,
      custom: undefined,
    };
  }
}
