import { Prop, propWithEffect } from 'common';
import debounce from 'common/debounce';
import * as xhr from 'common/xhr';
import { storedJsonProp, StoredJsonProp } from 'common/storage';

import {
  SetupConstraints,
  GameMode,
  GameType,
  InputValue,
  RealValue,
  SetupStore,
  TimeMode,
  ParentCtrl,
} from './interfaces';
import {
  daysVToDays,
  incrementVToIncrement,
  keyToId,
  sliderInitVal,
  timeModes,
  timeVToTime,
  variants,
} from './options';
import renderSetup from './view/modal';

const getPerf = (variant: VariantKey, timeMode: TimeMode, time: RealValue, increment: RealValue): Perf => {
  if (!['standard', 'fromPosition'].includes(variant)) return variant as Perf;
  if (timeMode !== 'realTime') return 'correspondence';

  const totalGameTime = time * 60 + increment * 40;
  return totalGameTime < 30
    ? 'ultraBullet'
    : totalGameTime < 180
    ? 'bullet'
    : totalGameTime < 480
    ? 'blitz'
    : totalGameTime < 1500
    ? 'rapid'
    : 'classical';
};

export class SetupCtrl {
  root: ParentCtrl;
  store: Record<GameType, StoredJsonProp<SetupStore>>;
  gameType: GameType | null = null;
  lastValidFen = '';
  fenError = false;
  friendUser = '';
  loading = false;
  blindModeColor: Prop<Color | 'random'>;

  // Store props
  variant: Prop<VariantKey>;
  fen: Prop<string>;
  timeMode: Prop<TimeMode>;
  gameMode: Prop<GameMode>;
  ratingMin: Prop<number>;
  ratingMax: Prop<number>;
  aiLevel: Prop<number>;

  // The following three quantities are suffixed with 'V' to draw attention to the
  // fact that they are not the true quantities. They represent the value of the
  // input element. Use time(), increment(), and days() below for the true quantities.
  timeV: Prop<InputValue>;
  incrementV: Prop<InputValue>;
  daysV: Prop<InputValue>;

  time: () => RealValue = () => timeVToTime(this.timeV());
  increment: () => RealValue = () => incrementVToIncrement(this.incrementV());
  days: () => RealValue = () => daysVToDays(this.daysV());

  constructor(ctrl: ParentCtrl) {
    this.root = ctrl;
    this.blindModeColor = propWithEffect('random', this.onPropChange);
    // Initialize stores with default props as necessary
    this.store = {
      hook: this.makeSetupStore('hook'),
      friend: this.makeSetupStore('friend'),
      ai: this.makeSetupStore('ai'),
      local: this.makeSetupStore('local'),
    };
  }

  // Namespace the store by username for user specific modal settings
  private storeKey = (gameType: GameType) => `lobby.setup.${this.root.user || 'anon'}.${gameType}`;

  makeSetupStore = (gameType: GameType) =>
    storedJsonProp<SetupStore>(this.storeKey(gameType), () => ({
      variant: 'standard',
      fen: '',
      timeMode: gameType === 'hook' ? 'realTime' : 'unlimited',
      time: 5,
      increment: 3,
      days: 2,
      gameMode: gameType === 'ai' || !this.root.user ? 'casual' : 'rated',
      ratingMin: -500,
      ratingMax: 500,
      aiLevel: 1,
    }));

  private init = (opts?: SetupConstraints) => {
    const storeProps = this.store[this.gameType!]();
    // Load props from the store, but override any store values with values found in opts
    this.variant = propWithEffect(opts?.variant || storeProps.variant, this.onDropdownChange);
    this.fen = this.propWithApply(opts?.fen || storeProps.fen);
    this.timeMode = propWithEffect(opts?.timeMode || storeProps.timeMode, this.onDropdownChange);
    this.timeV = this.propWithApply(sliderInitVal(storeProps.time, timeVToTime, 100)!);
    this.incrementV = this.propWithApply(sliderInitVal(storeProps.increment, incrementVToIncrement, 100)!);
    this.daysV = this.propWithApply(sliderInitVal(storeProps.days, daysVToDays, 20)!);
    this.gameMode = this.propWithApply(storeProps.gameMode);
    this.ratingMin = this.propWithApply(storeProps.ratingMin);
    this.ratingMax = this.propWithApply(storeProps.ratingMax);
    this.aiLevel = this.propWithApply(storeProps.aiLevel);

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
      timeMode: this.timeMode(),
      time: this.time(),
      increment: this.increment(),
      days: this.days(),
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

  private isProvisional = () => {
    const rating = this.root.ratingMap && this.root.ratingMap[this.selectedPerf()];
    return rating ? !!rating.prov : true;
  };

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

  openModal = (gameType: GameType, opts?: SetupConstraints, friendUser?: string) => {
    this.gameType = gameType;
    this.loading = false;
    this.fenError = false;
    this.lastValidFen = '';
    this.friendUser = friendUser || '';
    this.init(opts);
    this.root.redraw();
  };

  renderModal = () => renderSetup(this);

  closeModal = () => {
    this.gameType = null;
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
    !this.root.user ||
    // unlimited games cannot be rated
    this.timeMode() === 'unlimited' ||
    // variants with very low time cannot be rated
    (this.variant() !== 'standard' &&
      (this.timeMode() !== 'realTime' ||
        (this.time() < 0.5 && this.increment() == 0) ||
        (this.time() == 0 && this.increment() < 2)));

  selectedPerf = (): Perf => getPerf(this.variant(), this.timeMode(), this.time(), this.increment());

  ratingRange = (): string => {
    if (!this.root.ratingMap) return '';
    const rating = this.root.ratingMap[this.selectedPerf()].rating;
    return `${Math.max(100, rating + this.ratingMin())}-${rating + this.ratingMax()}`;
  };

  propsToFormData = (color: Color | 'random'): FormData =>
    xhr.form({
      variant: keyToId(this.variant(), variants).toString(),
      fen: this.fen(),
      timeMode: keyToId(this.timeMode(), timeModes(this.root.trans)).toString(),
      time: this.time().toString(),
      time_range: this.timeV().toString(),
      increment: this.increment().toString(),
      increment_range: this.incrementV().toString(),
      days: this.days().toString(),
      days_range: this.daysV().toString(),
      mode: this.gameMode() === 'casual' ? '0' : '1',
      ratingRange: this.ratingRange(),
      ratingRange_range_min: this.ratingMin().toString(),
      ratingRange_range_max: this.ratingMax().toString(),
      level: this.aiLevel().toString(),
      color,
    });

  validFen = (): boolean => this.variant() !== 'fromPosition' || (!this.fenError && !!this.fen());
  validTime = (): boolean => this.timeMode() !== 'realTime' || this.time() > 0 || this.increment() > 0;
  validAiTime = (): boolean =>
    this.gameType !== 'ai' ||
    this.timeMode() !== 'realTime' ||
    this.variant() !== 'fromPosition' ||
    this.time() >= 1;
  valid = (): boolean => this.validFen() && this.validTime() && this.validAiTime();

  submit = async (color: Color | 'random') => {
    if (
      this.root.acquire?.({
        id: `${this.time()}+${this.increment()}`,
        gameType: this.gameType,
        color,
        variant: this.variant(),
        gameMode: this.gameMode(),
        timeMode: this.timeMode(),
        range: this.ratingRange(),
      })
    ) {
      this.closeModal();
      return;
    }
    this.loading = true;
    this.root.redraw();

    let urlPath = `/setup/${this.gameType}`;
    console.log(urlPath);
    if (this.gameType === 'hook') urlPath += '/' + lichess.sri;
    const urlParams = { user: this.friendUser || undefined };
    let response;
    try {
      response = await xhr.textRaw(xhr.url(urlPath, urlParams), {
        method: 'post',
        body: this.propsToFormData(color),
      });
    } catch (e) {
      this.loading = false;
      this.root.redraw();
      alert('Sorry, we encountered an error while creating your game. Please try again.');
      return;
    }

    const { ok, redirected, url } = response;

    if (!ok) {
      const errs: { [key: string]: string } = await response.json();
      alert(
        errs
          ? Object.keys(errs)
              .map(k => `${k}: ${errs[k]}`)
              .join('\n')
          : 'Invalid setup',
      );
      if (response.status == 403) {
        // 403 FORBIDDEN closes this modal because challenges to the recipient
        // will not be accepted.  see friend() in controllers/Setup.scala
        this.closeModal();
      }
    } else if (redirected) {
      location.href = url;
    } else {
      this.loading = false;
      this.closeModal();
    }
  };
}
