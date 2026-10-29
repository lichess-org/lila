import { type Prop, prop, propWithEffect } from 'lib';
import { type BotInfo } from 'lib/bot/types';
import type { ClockConfig } from 'lib/game/clock/clockCtrl';
import type { ColorChoice, ColorProp } from 'lib/setup/color';
import { type TimeControl, timeControlFromStoredValues } from 'lib/setup/timeControl';
import { storedJsonProp } from 'lib/storage';
import type { Dialog } from 'lib/view';

import { Game } from '../game';
import type { Bot, BotOpts } from '../interfaces';
import { setupDialog } from './view/setupDialog';

interface Settings {
  color: ColorChoice;
  clock: boolean;
  time: Minutes;
  increment: Seconds;
}

export default class SetupCtrl {
  timeControl: TimeControl;
  settings: Prop<Settings> = storedJsonProp<Settings>('botPlay.setup.settings', () => ({
    color: 'random',
    clock: false,
    time: 5,
    increment: 3,
  }));
  color: ColorProp;
  dialog?: Dialog;

  constructor(
    readonly opts: BotOpts,
    private readonly ongoing: () => Game | undefined,
    readonly resume: () => void,
    private readonly start: (bot: BotInfo, pov: ColorChoice, clock?: ClockConfig) => void,
    readonly redraw: () => void,
  ) {
    const s = this.settings();
    this.color = prop(s.color);
    this.timeControl = timeControlFromStoredValues(
      propWithEffect(s.clock ? 'realTime' : 'unlimited', () => this.dialog?.redraw()),
      ['realTime', 'unlimited'],
      s.time,
      s.increment,
      0,
      () => this.dialog?.redraw(),
      [],
    );
  }

  select = async (bot: Bot) => {
    this.dialog = await setupDialog(this, bot);
  };

  play = (bot: Bot) => {
    this.saveSettings();
    this.dialog?.close();
    this.start(bot, this.color(), clockConfig(this.timeControl));
  };

  private readonly saveSettings = () => {
    this.settings({
      color: this.color(),
      clock: this.timeControl.mode() === 'realTime',
      time: this.timeControl.time(),
      increment: this.timeControl.increment(),
    });
  };

  ongoingGameWorthResuming = () => {
    const game = this.ongoing();
    if (!game?.worthResuming()) return undefined;
    const bot = this.opts.bots.find(b => b.key === game.botKey);
    if (!bot) return undefined;
    return { game, board: game.lastBoard(), bot };
  };
}

const clockConfig = (tc: TimeControl): ClockConfig | undefined =>
  tc.isRealTime() && tc.realTimeValid()
    ? {
        initial: tc.initialSeconds(),
        increment: tc.increment(),
        moretime: 0,
      }
    : undefined;
