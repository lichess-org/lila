import { Prop, propWithEffect } from 'common';
import debounce from 'common/debounce';
import * as xhr from 'common/xhr';
import { storedJsonProp, StoredJsonProp } from 'common/storage';
import LobbyController from './ctrl';
import { ForceSetupOptions, GameMode, GameType, PoolMember, SetupStore, TimeMode } from './interfaces';
import {
  daysVToDays,
  incrementVToIncrement,
  keyToId,
  sliderInitVal,
  timeModes,
  timeVToTime,
  variants,
} from './options';

const perfOrSpeed = (variant: VariantKey, timeMode: TimeMode, time: number, increment: number): Perf | Speed => {
  if (!['standard', 'fromPosition'].includes(variant)) return variant as Perf;
  if (timeMode !== 'realTime') return 'correspondence';

  const totalGameTime = time * 60 + increment * 40;
  if (totalGameTime < 30) return 'ultraBullet';
  else if (totalGameTime < 180) return 'bullet';
  else if (totalGameTime < 480) return 'blitz';
  else if (totalGameTime < 1500) return 'rapid';
  else return 'classical';
};

export default class SetupController {
  root: LobbyController;
  store: Partial<Record<GameType, StoredJsonProp<SetupStore>>> = {};
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
  timeV: Prop<number>;
  incrementV: Prop<number>;
  daysV: Prop<number>;

  time: () => number = () => timeVToTime(this.timeV());
  increment: () => number = () => incrementVToIncrement(this.incrementV());
  days: () => number = () => daysVToDays(this.daysV());

  constructor(ctrl: LobbyController) {
    this.root = ctrl;
    this.blindModeColor = propWithEffect('random', this.onPropChange);

    // Initialize stores with default props as necessary
    for (const gameType of ['hook', 'friend', 'ai']) {
      this.store[gameType as GameType] = storedJsonProp(`lobby.gameSetup.${gameType}`, () =>
        this.getDefaultProps(gameType as GameType)
      );
    }
  }

  getDefaultProps = (gameType: GameType): SetupStore => ({
    variant: 'standard',
    fen: '',
    timeMode: gameType === 'hook' ? 'realTime' : 'unlimited',
    time: 5,
    increment: 3,
    days: 2,
    gameMode: gameType === 'ai' || !this.root.data.me ? 'casual' : 'rated',
    ratingMin: -500,
    ratingMax: 500,
    aiLevel: 1,
  });

  savePropsToStore = () =>
    this.store[this.gameType!]!({
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
    });

  loadPropsFromStore = (forceOptions?: ForceSetupOptions) => {
    const storeProps = this.store[this.gameType!]!();
    // Load props from the store, but override any store values with values found in forceOptions
    this.variant = propWithEffect(forceOptions?.variant ? forceOptions.variant : storeProps.variant, this.onPropChange);
    this.fen = propWithEffect(forceOptions?.fen ? forceOptions.fen : storeProps.fen, this.onPropChange);
    this.timeMode = propWithEffect(
      forceOptions?.timeMode ? forceOptions.timeMode : storeProps.timeMode,
      this.onPropChange
    );
    this.timeV = propWithEffect(sliderInitVal(storeProps.time, timeVToTime, 100)!, this.onPropChange);
    this.incrementV = propWithEffect(
      sliderInitVal(storeProps.increment, incrementVToIncrement, 100)!,
      this.onPropChange
    );
    this.daysV = propWithEffect(sliderInitVal(storeProps.days, daysVToDays, 20)!, this.onPropChange);
    this.gameMode = propWithEffect(storeProps.gameMode, this.onPropChange);
    this.ratingMin = propWithEffect(storeProps.ratingMin, this.onPropChange);
    this.ratingMax = propWithEffect(storeProps.ratingMax, this.onPropChange);
    this.aiLevel = propWithEffect(storeProps.aiLevel, this.onPropChange);

    // Upon loading the props from the store, immediately call onPropChange. This has some important
    // effects:
    // 1. Overridden props will now be saved. This way, the user can know that whatever they saw
    //    last in the modal will be there when they open it at a later time.
    // 2. Prop validity will be checked. There is at least one settled case where this will matter.
    //    If a user is logged in and playing rated hook games, logs out, and then attempts to play
    //    a hook game as anonymous via this modal _without changing any props_, they would be attempting
    //    something unallowed: rated games as anonymous. This way, we ensure that props are always valid
    //    upon load.
    this.onPropChange();
  };

  onPropChange = () => {
    if (this.gameMode() === 'rated' && this.ratedModeDisabled()) {
      // reassign with propWithEffect here to avoid calling this.onPropChange
      this.gameMode = propWithEffect('casual', this.onPropChange);
    }
    this.savePropsToStore();
    this.root.redraw();
  };

  openModal = (gameType: GameType, forceOptions?: ForceSetupOptions, friendUser?: string) => {
    this.root.leavePool();
    this.gameType = gameType;
    this.loading = false;
    this.fenError = false;
    this.lastValidFen = '';
    this.friendUser = friendUser || '';
    this.loadPropsFromStore(forceOptions);
  };

  closeModal = () => {
    this.gameType = null;
    this.root.redraw();
  };

  validateFen = debounce(() => {
    const fen = this.fen();
    if (!fen) return;
    xhr.text(xhr.url('/setup/validate-fen', { fen, strict: this.gameType === 'ai' ? 1 : undefined })).then(
      () => {
        this.fenError = false;
        this.lastValidFen = fen;
        this.root.redraw();
      },
      () => {
        this.fenError = true;
        this.root.redraw();
      }
    );
  }, 200);

  ratedModeDisabled = (): boolean => {
    return (
      // anonymous games cannot be rated
      !this.root.data.me ||
      // unlimited games cannot be rated
      (this.gameType === 'hook' && this.timeMode() === 'unlimited') ||
      // variants with very low time cannot be rated
      (this.variant() !== 'standard' &&
        (this.timeMode() !== 'realTime' ||
          (this.time() < 0.5 && this.increment() == 0) ||
          (this.time() == 0 && this.increment() < 2)))
    );
  };

  selectedPerfOrSpeed = () => perfOrSpeed(this.variant(), this.timeMode(), this.time(), this.increment());

  ratingRange = (): string => {
    if (!this.root.data.ratingMap) return '';
    const rating = this.root.data.ratingMap[this.selectedPerfOrSpeed()];
    return `${rating + this.ratingMin()}-${rating + this.ratingMax()}`;
  };

  // This function is a special version of the above function that does not require the modal to
  // be opened. Used in boot.ts for hook-like games ("New opponent" button).
  hookRatingRange = (): string => {
    if (!this.root.data.ratingMap) return '';
    const hookStore = storedJsonProp('lobby.gameSetup.hook', () => this.getDefaultProps('hook'));
    const { variant, timeMode, time, increment, ratingMin, ratingMax } = hookStore();
    const rating = this.root.data.ratingMap[perfOrSpeed(variant, timeMode, time, increment)];
    return `${rating + ratingMin}-${rating + ratingMax}`;
  };

  hookToPoolMember = (color: Color | 'random'): PoolMember | null => {
    const valid =
      color == 'random' && this.variant() == 'standard' && this.gameMode() == 'rated' && this.timeMode() == 'realTime';
    const id = `${this.time()}+${this.increment()}`;
    return valid && this.root.pools.find(p => p.id === id)
      ? {
          id,
          range: this.ratingRange(),
        }
      : null;
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

  submit = async (color: Color | 'random') => {
    const poolMember = this.hookToPoolMember(color);
    if (poolMember) {
      this.root.enterPool(poolMember);
      this.closeModal();
      return;
    }

    if (this.gameType === 'hook') this.root.setTab(this.timeMode() === 'realTime' ? 'real_time' : 'seeks');
    this.loading = true;
    this.root.redraw();

    let urlPath = `/setup/${this.gameType}`;
    if (this.gameType === 'hook') urlPath += `/${lichess.sri}`;
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
      alert('Sorry, we encountered in error while creating your game. Please try again.');
      return;
    }

    if (response.redirected) {
      location.href = response.url;
    }

    this.loading = false;
    this.closeModal();
  };
}
