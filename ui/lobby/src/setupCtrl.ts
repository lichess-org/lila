import { type Prop, propWithEffect, toggle } from 'lib';
import { debounce } from 'lib/async';
import * as xhr from 'lib/xhr';
import { storedJsonProp } from 'lib/storage';
import { alert } from 'lib/view';
import { INITIAL_FEN } from 'chessops/fen';
import type LobbyController from './ctrl';
import type { ForceSetupOptions, GameMode, GameType, PoolMember, SetupStore } from './interfaces';
import { keyToId, variants } from './options';
import {
  allTimeModeKeys,
  timeControlFromStoredValues,
  timeModes,
  type TimeControl,
} from 'lib/setup/timeControl';
import type { ColorChoice, ColorProp } from 'lib/setup/color';

const getPerf = (variant: VariantKey, tc: TimeControl): Perf =>
  variant !== 'standard' && variant !== 'fromPosition' ? variant : tc.speed();

export default class SetupController {
  root: LobbyController;
  store: Record<GameType, Prop<SetupStore>>;
  gameType: GameType | null = null;
  lastValidFen = '';
  fenError = false;
  friendUser = '';
  loading = false;
  color: ColorProp;

  // Store props
  variant: Prop<VariantKey>;
  fen: Prop<string>;
  gameMode: Prop<GameMode>;
  ratingMin: Prop<number>;
  ratingMax: Prop<number>;
  aiLevel: Prop<number>;

  variantMenuOpen = toggle(false);

  timeControl: TimeControl;

  constructor(ctrl: LobbyController) {
    this.root = ctrl;
    this.color = propWithEffect('random', this.onPropChange);
    // Initialize stores with default props as necessary
    this.store = {
      hook: this.makeSetupStore('hook'),
      friend: this.makeSetupStore('friend'),
      ai: this.makeSetupStore('ai'),
    };
  }

  // Namespace the store by username for user specific modal settings
  private storeKey = (gameType: GameType) => `lobby.setup.${this.root.me?.username || 'anon'}.${gameType}`;

  makeSetupStore = (gameType: GameType) =>
    storedJsonProp<SetupStore>(this.storeKey(gameType), () => ({
      variant: 'standard',
      fen: '',
      timeMode: gameType === 'hook' ? 'realTime' : 'unlimited',
      time: 5,
      increment: 3,
      days: 2,
      gameMode: gameType === 'ai' || !this.root.me ? 'casual' : 'rated',
      ratingMin: -500,
      ratingMax: 500,
      aiLevel: 1,
    }));

  private loadPropsFromStore = (forceOptions?: ForceSetupOptions) => {
    const storeProps = this.store[this.gameType!]();
    // Load props from the store, but override any store values with values found in forceOptions
    this.variant = propWithEffect(forceOptions?.variant || storeProps.variant, this.onDropdownChange);
    this.fen = this.propWithApply(forceOptions?.fen || storeProps.fen);
    const canChangeTimeMode = !!this.root.me || this.gameType !== 'hook';
    this.timeControl = timeControlFromStoredValues(
      propWithEffect(forceOptions?.timeMode || storeProps.timeMode, this.onDropdownChange),
      canChangeTimeMode ? allTimeModeKeys : ['realTime'],
      forceOptions?.time ?? storeProps.time,
      forceOptions?.increment ?? storeProps.increment,
      forceOptions?.days ?? storeProps.days,
      this.onPropChange,
      this.root.pools,
    );
    this.gameMode = this.propWithApply(forceOptions?.mode ?? storeProps.gameMode);
    this.ratingMin = this.propWithApply(storeProps.ratingMin);
    this.ratingMax = this.propWithApply(storeProps.ratingMax);
    this.aiLevel = this.propWithApply(storeProps.aiLevel);
    this.color(forceOptions?.color || 'random');

    this.enforcePropRules();
    // Upon loading the props from the store, overriding with forced options, and enforcing rules,
    // immediately save them to the store. This way, the user can know that whatever they saw last
    // in the modal will be there when they open it at a later time.
    this.savePropsToStore();
  };

  private enforcePropRules = () => {
    // reassign with this.propWithApply in this function to avoid calling this.onPropChange

    // replace underscores with spaces in FEN
    if (this.variant() === 'fromPosition') this.fen = this.propWithApply(this.fen().replace(/_/g, ' '));

    if (this.gameMode() === 'rated' && this.ratedModeDisabled()) {
      this.gameMode = this.propWithApply('casual');
    }

    this.ratingMin = this.propWithApply(Math.min(0, this.ratingMin()));
    this.ratingMax = this.propWithApply(Math.max(0, this.ratingMax()));
    if (this.ratingMin() === 0 && this.ratingMax() === 0) {
      this.ratingMax = this.propWithApply(50);
    }
  };

  private savePropsToStore = (override: Partial<SetupStore> = {}) =>
    this.gameType &&
    this.store[this.gameType]({
      variant: this.variant(),
      fen: this.fen(),
      timeMode: this.timeControl.mode(),
      time: this.timeControl.time(),
      increment: this.timeControl.increment(),
      days: this.timeControl.days(),
      gameMode: this.gameMode(),
      ratingMin: this.ratingMin(),
      ratingMax: this.ratingMax(),
      aiLevel: this.aiLevel(),
      ...override,
    });

  private savePropsToStoreExceptRating = () =>
    this.gameType &&
    this.savePropsToStore({
      ratingMin: this.store[this.gameType]().ratingMin,
      ratingMax: this.store[this.gameType]().ratingMax,
    });

  myRating = () => this.root.data.ratingMap && Math.abs(this.root.data.ratingMap[this.selectedPerf()]);
  isProvisional = () => (this.root.data.ratingMap ? this.root.data.ratingMap[this.selectedPerf()] < 0 : true);

  private onPropChange = () => {
    if (this.isProvisional()) this.savePropsToStoreExceptRating();
    else this.savePropsToStore();
    this.root.redraw();
  };

  private onDropdownChange = () => {
    // Handle rating update here
    this.enforcePropRules();
    if (this.isProvisional()) {
      this.ratingMin(-500);
      this.ratingMax(500);
      this.savePropsToStoreExceptRating();
    } else {
      if (this.gameType) {
        this.ratingMin(this.store[this.gameType]().ratingMin);
        this.ratingMax(this.store[this.gameType]().ratingMax);
      }
      this.savePropsToStore();
    }
    this.root.redraw();
  };

  private propWithApply = <A>(value: A) => propWithEffect(value, this.onPropChange);

  openModal = (
    gameType: Exclude<GameType, 'local'>,
    forceOptions?: ForceSetupOptions,
    friendUser?: string,
  ) => {
    this.root.leavePool();
    this.gameType = gameType;
    this.loading = false;
    this.fenError = false;
    this.lastValidFen = '';
    this.friendUser = friendUser || '';
    this.variantMenuOpen(false);
    this.loadPropsFromStore(forceOptions);
  };

  closeModal?: () => void; // managed by view/setup/modal.ts

  toggleVariantMenu = () => {
    this.variantMenuOpen.toggle();
    this.root.redraw();
  };

  validateFen = debounce(() => {
    const fen = this.fen();
    if (!fen) return;
    xhr
      .text(
        xhr.url('/setup/validate-fen', {
          fen,
          strict: this.gameType === 'ai' ? 1 : undefined,
        }),
      )
      .then(
        () => {
          this.fenError = false;
          this.lastValidFen = fen;
          this.root.redraw();
        },
        () => {
          this.fenError = true;
          this.root.redraw();
        },
      );
  }, 300);

  ratedModeDisabled = (): boolean =>
    // anonymous games cannot be rated
    !this.root.me ||
    this.timeControl.mode() === 'unlimited' ||
    (this.variant() === 'fromPosition' && this.fen() !== INITIAL_FEN) ||
    // variants with very low time cannot be rated
    (this.variant() !== 'standard' && this.timeControl.notForRatedVariant());

  selectedPerf = (): Perf => getPerf(this.variant(), this.timeControl);

  ratingRange = (): string => {
    const rating = this.myRating();
    return rating ? `${Math.max(100, rating + this.ratingMin())}-${rating + this.ratingMax()}` : '';
  };

  hookToPoolMember = (color: ColorChoice): PoolMember | null => {
    const valid =
      color === 'random' &&
      this.gameType === 'hook' &&
      this.variant() === 'standard' &&
      this.gameMode() === 'rated' &&
      this.timeControl.isRealTime();
    const id = this.timeControl.clockStr();
    return valid && this.root.pools.find(p => p.id === id)
      ? {
          id,
          range: this.ratingRange(),
        }
      : null;
  };

  propsToFormData = (color: ColorChoice): FormData =>
    xhr.form({
      variant: keyToId(this.variant(), variants).toString(),
      fen: this.variant() === 'fromPosition' ? this.fen() : undefined,
      timeMode: keyToId(this.timeControl.mode(), timeModes).toString(),
      time: this.timeControl.time().toString(),
      time_range: this.timeControl.timeV().toString(),
      increment: this.timeControl.increment().toString(),
      increment_range: this.timeControl.incrementV().toString(),
      days: this.timeControl.days().toString(),
      days_range: this.timeControl.daysV().toString(),
      mode: this.gameMode() === 'casual' ? '0' : '1',
      ratingRange: this.ratingRange(),
      ratingRange_range_min: this.ratingMin().toString(),
      ratingRange_range_max: this.ratingMax().toString(),
      level: this.aiLevel().toString(),
      color,
    });

  validFen = (): boolean => this.variant() !== 'fromPosition' || (!this.fenError && !!this.fen());

  valid = (): boolean => this.validFen() && this.timeControl.valid(this.minimumTimeIfReal());

  minimumTimeIfReal = (): number => (this.gameType === 'ai' && this.variant() === 'fromPosition' ? 1 : 0);

  submit = async () => {
    const color = this.color();
    const poolMember = this.hookToPoolMember(color);
    if (poolMember) {
      this.root.enterPool(poolMember);
      this.closeModal?.();
      return;
    }

    if (this.gameType === 'hook') this.root.setTab(this.timeControl.isRealTime() ? 'real_time' : 'seeks');
    this.loading = true;
    this.root.redraw();

    let urlPath = `/setup/${this.gameType}`;
    if (this.gameType === 'hook') urlPath += `/${site.sri}`;
    const urlParams = { user: this.friendUser || undefined };
    let response;
    try {
      response = await xhr.textRaw(xhr.url(urlPath, urlParams), {
        method: 'post',
        body: this.propsToFormData(color),
      });
    } catch (_) {
      this.loading = false;
      this.root.redraw();
      alert('Sorry, we encountered an error while creating your game. Please try again.');
      return;
    }

    const { ok, redirected, url } = response;

    if (!ok) {
      const errs: { [key: string]: string } = await response.json();
      await alert(
        errs
          ? Object.keys(errs)
              .map(k => `${k}: ${errs[k]}`)
              .join('\n')
          : 'Invalid setup',
      );
      if (response.status === 403) {
        // 403 FORBIDDEN closes this modal because challenges to the recipient
        // will not be accepted.  see friend() in controllers/Setup.scala
        this.closeModal?.();
      }
    } else if (redirected) {
      location.href = url;
    } else {
      this.loading = false;
      this.closeModal?.();
    }
  };
}
