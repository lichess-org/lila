import { BotOpts, Game } from '../interfaces';
import { BotInfo } from 'local';
import { makeBoardAt } from '../chess';

export default class SetupCtrl {
  constructor(
    readonly opts: BotOpts,
    private readonly ongoing: () => Game | null,
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
    return { game, board: makeBoardAt(game), bot };
  };
}
