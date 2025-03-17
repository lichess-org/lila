import { BotOpts } from '../interfaces';
import { BotInfo } from 'local';

export default class SetupCtrl {
  dialogBot: BotInfo | undefined;

  constructor(
    readonly opts: BotOpts,
    private readonly start: (bot: BotInfo, pov: Color | undefined) => void,
    readonly redraw: () => void,
  ) {}

  play = (bot: BotInfo) => {
    this.dialogBot = undefined;
    this.start(bot, Math.random() < 0.5 ? 'white' : 'black');
  };
}
