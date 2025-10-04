import type { Bot, BotOpts } from '../interfaces';
import { type BotInfo } from 'lib/bot/types';
import { Game } from '../game';
import type { ColorOrRandom, ColorProp } from 'lib/setup/interfaces';
import { prop } from 'lib';

export default class SetupCtrl {
  selectedBot?: Bot;
  color: ColorProp = prop('random' as ColorOrRandom);

  constructor(
    readonly opts: BotOpts,
    private readonly ongoing: () => Game | undefined,
    readonly resume: () => void,
    private readonly start: (bot: BotInfo, pov: Color | undefined) => void,
    readonly redraw: () => void,
  ) {
    this.selectedBot = this.opts.bots[0];
  }

  select = (bot: Bot) => {
    this.selectedBot = bot;
    this.redraw();
  };

  cancel = () => {
    this.selectedBot = undefined;
    this.redraw();
  };

  play = () => {
    if (!this.selectedBot) return;
    this.start(this.selectedBot, Math.random() < 0.5 ? 'white' : 'black');
  };

  ongoingGameWorthResuming = () => {
    const game = this.ongoing();
    if (!game?.worthResuming()) return;
    const bot = this.opts.bots.find(b => b.key === game.botKey);
    if (!bot) return;
    return { game, board: game.lastBoard(), bot };
  };
}
