import { BotOpts } from '../interfaces';
import { type BotInfo } from 'lib/bot/types';
import { Game } from '../game';

export default class SetupCtrl {
  constructor(
    readonly opts: BotOpts,
    private readonly ongoing: () => Game | undefined,
    readonly resume: () => void,
    private readonly start: (bot: BotInfo, pov: Color | undefined) => void,
    readonly redraw: () => void,
  ) {}

  play = (bot: BotInfo) => {
    this.start(bot, Math.random() < 0.5 ? 'white' : 'black');
  };

  ongoingGame = () => {
    const game = this.ongoing();
    if (!game) return;
    const bot = this.opts.bots.find(b => b.uid === game.botId);
    if (!bot) return;
    return { game, board: game.lastBoard(), bot };
  };
}
