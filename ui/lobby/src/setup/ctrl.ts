import { clockEstimateSeconds, clockToPerf } from 'common/clock';
import { idToVariant, variantToId } from 'common/variant';
import { engineName } from 'shogi/engine-name';
import { RULES } from 'shogiops';
import { findHandicaps, isHandicap } from 'shogiops/handicaps';
import { parseSfen } from 'shogiops/sfen';
import type LobbyController from '../ctrl';
import { type FormStore, makeStore } from '../form';
import {
  Mode,
  Position,
  TimeMode,
  aiLevelChoices,
  byoChoices,
  dayChoices,
  incChoices,
  maxRatingChoices,
  minRatingChoices,
  modeChoices,
  periodChoices,
  positionChoices,
  timeChoices,
  timeModeChoices,
} from './util';

export default class SetupCtrl {
  key: SetupKey;
  data: SetupData;

  stores: {
    hook: FormStore;
    friend: FormStore;
    ai: FormStore;
  };

  isOpen: boolean;
  isExtraOpen: boolean;

  invalidSfen: boolean;

  nvui: boolean;

  constructor(public root: LobbyController) {
    this.stores = {
      hook: makeStore(window.lishogi.storage.make('lobby.setup.hook')),
      friend: makeStore(window.lishogi.storage.make('lobby.setup.friend')),
      ai: makeStore(window.lishogi.storage.make('lobby.setup.ai')),
    };
    this.nvui = root.opts.blindMode;
  }

  redraw(): void {
    this.root.redraw();
  }

  set(key: string, value: string | number | boolean): void {
    if (key === 'handicap') {
      this.data.sfen = value as string;
    }
    if (key === 'sfen') {
      this.data.handicap = '';
    }
    if (key === 'variant') {
      this.data.sfen = '';
      this.data.handicap = '';
      this.data.position = Position.initial;
    }

    this.data[key] = value;

    this.updateData();
    console.log('data after set:', this.data);

    this.redraw();
  }

  selected(key: string): string | number {
    return this.data[key];
  }

  isCorres(): boolean {
    return this.data.timeMode == TimeMode.Corres;
  }

  hasSfen(): boolean {
    return !!this.data.sfen && this.data.position === Position.fromPosition;
  }

  isHandicap(): boolean {
    return this.data.sfen ? isHandicap({ sfen: this.data.sfen, rules: this.variantKey() }) : false;
  }

  validateSfen(): void {
    this.invalidSfen = parseSfen(this.variantKey(), this.data.sfen, true).isErr;
  }

  variantKey(): VariantKey {
    return idToVariant(this.data.variant);
  }

  timeSum(): number {
    return clockEstimateSeconds(
      this.data.time * 60,
      this.data.byoyomi,
      this.data.increment,
      this.data.periods,
    );
  }

  canBeRated(): boolean {
    return !(
      this.hasSfen() ||
      (this.key === 'hook' && this.data.timeMode == TimeMode.Unlimited) ||
      (this.data.timeMode == TimeMode.RealTime &&
        (this.data.periods > 1 || (this.data.increment > 0 && this.data.byoyomi > 0))) ||
      (this.data.variant == 3 && this.timeSum() < 250)
    );
  }

  validTime(): boolean {
    return (
      this.data.timeMode !== TimeMode.RealTime ||
      ((this.data.time > 0 || this.data.increment > 0 || this.data.byoyomi > 0) &&
        (this.data.byoyomi > 0 || this.data.periods === 1))
    );
  }

  canChooseColor(): boolean {
    return this.key !== 'hook' || this.data.mode !== Mode.Rated;
  }

  canSubmit(): boolean {
    const timeOk = this.validTime();
    const ratedOk = this.data.mode != Mode.Rated || this.canBeRated();
    const aiOk =
      this.key !== 'ai' ||
      this.data.variant === 1 ||
      this.data.time >= 1 ||
      this.data.byoyomi >= 10 ||
      this.data.increment >= 5;

    return !this.invalidSfen && timeOk && ratedOk && aiOk;
  }

  engineName(): string {
    const sfen = this.data.sfen;
    const rules = idToVariant(this.data.variant);
    const level = this.data.level;

    return engineName(rules, sfen, level);
  }

  perf(): Perf | undefined {
    const v = this.variantKey();
    if (v === 'standard') {
      if (!this.validTime()) return;
      else if (this.isCorres()) return 'correspondence';
      else {
        return clockToPerf(
          this.data.time * 60,
          this.data.byoyomi,
          this.data.increment,
          this.data.periods,
        );
      }
    } else return v;
  }

  rating(): number | undefined {
    if (!this.root.ratings) return;

    const p = this.perf();
    return p ? this.root.ratings[p]?.rating : undefined;
  }

  ratingRange(): string | undefined {
    const rating = this.rating();
    if (rating) return `${rating - this.data.ratingMin}-${rating + this.data.ratingMin}`;
    else return;
  }

  initData(key: SetupKey, extraData?: Record<string, string>): void {
    this.key = key;

    const store = this.stores[this.key]?.get() || {};

    const getNumber = (k: keyof SetupData, options: number[]): number => {
      const extra = extraData?.[k] ? Number.parseInt(extraData[k]) : undefined;
      const saved = extra ?? Number.parseInt(store[k]);
      if (saved !== null && saved !== undefined && !Number.isNaN(saved) && options.includes(saved))
        return saved;
      else return SetupCtrl.defaultData[k] as number;
    };

    const getString = (k: keyof SetupData, options: string[] | undefined = undefined): string => {
      const extra = extraData?.[k];
      const saved = extra ?? store[k];
      if (saved !== null && saved !== undefined && (!options || options.includes(saved)))
        return saved;
      else return SetupCtrl.defaultData[k] as string;
    };

    try {
      const variantId = getNumber(
        'variant',
        RULES.map(r => variantToId(r)),
      );
      this.data = {
        variant: variantId,
        timeMode: getNumber('timeMode', timeModeChoices),
        time: getNumber('time', timeChoices),
        byoyomi: getNumber('byoyomi', byoChoices),
        increment: getNumber('increment', incChoices),
        periods: getNumber('periods', periodChoices),
        days: getNumber('days', dayChoices),
        position: getNumber('position', positionChoices),
        sfen: getString('sfen'),
        handicap: getString(
          'sfen',
          findHandicaps({ rules: idToVariant(variantId) }).map(h => h.sfen),
        ),
        level: getNumber('level', aiLevelChoices),
        ratingMin: getNumber('ratingMin', minRatingChoices),
        ratingMax: getNumber('ratingMax', maxRatingChoices),
        mode: getNumber('mode', modeChoices),
      };
    } catch (e) {
      console.error(e);
      this.data = SetupCtrl.defaultData;
    }

    this.updateData();

    this.isExtraOpen = this.data.periods > 1 || this.data.increment > 0;
  }

  updateData(): void {
    if (!this.canBeRated()) this.data.mode = Mode.Casual;

    if (this.data.position === Position.initial) this.invalidSfen = false;
  }

  open(key: SetupKey, extraData?: Record<string, string>): void {
    console.log('store hook:', this.stores.hook.get());
    console.log('store friend:', this.stores.friend.get());
    console.log('store ai:', this.stores.ai.get());
    this.initData(key, extraData);

    console.log('data:', this.data);

    this.isOpen = true;
    this.redraw();
  }

  close(): void {
    this.save();
    this.isOpen = false;
    document
      .querySelectorAll('.lobby__start button.active')
      .forEach(el => el.classList.remove('active'));
    this.redraw();
  }

  toggleExtra(): void {
    this.isExtraOpen = !this.isExtraOpen;

    if (!this.isExtraOpen) {
      this.data.periods = 1;
      this.data.increment = 0;
    }

    this.redraw();
  }

  save = (): void => {
    this.stores[this.key].set(this.data as any);
  };

  submit = (color: Color | 'random'): void => {
    const rating = this.rating();
    const postData = {
      variant: this.data.variant,
      timeMode: this.data.timeMode,
      time: this.data.time,
      byoyomi: this.data.byoyomi,
      increment: this.data.increment,
      periods: this.data.periods,
      days: this.data.days,
      sfen: this.data.sfen,
      level: this.data.level,
      mode: this.data.mode,
      ratingRange: this.ratingRange(),
      color: color,
    };
    console.log(
      rating ? `${rating - this.data.ratingMin}-${rating + this.data.ratingMin}` : undefined,
    );

    let url = `/setup/${this.key}`;
    if (this.key === 'hook') url += `/${window.lishogi.sri}`;

    window.lishogi.xhr
      .text('POST', url, { formData: postData })
      .catch(e => alert(`Failed to create game - ${e}`));

    if (this.key === 'hook') this.root.setTab(this.isCorres() ? 'seeks' : 'real_time');

    this.close();
  };

  static defaultData: SetupData = {
    variant: variantToId('standard'),
    timeMode: TimeMode.RealTime,
    time: 30,
    byoyomi: 10,
    increment: 0,
    periods: 1,
    days: 1,
    position: Position.initial,
    handicap: '',
    sfen: '',
    level: 1,
    ratingMin: 500,
    ratingMax: 500,
    mode: Mode.Casual,
  };
}

export type SetupKey = 'hook' | 'friend' | 'ai';

export interface SetupData {
  variant: number;
  timeMode: number;
  time: number;
  byoyomi: number;
  increment: number;
  periods: number;
  days: number;
  position: number;
  sfen: string;
  handicap: string;
  level: number;
  mode: number;
  ratingMin: number;
  ratingMax: number;
}

export type SetupDataKey = keyof SetupData;
