import { BotOpts } from '../interfaces';
import { BotInfo } from 'local';

export default class SetupCtrl {
  dialogBot: BotInfo | undefined;

  constructor(
    readonly opts: BotOpts,
    private readonly start: (bot: BotInfo) => void,
    readonly redraw: () => void,
  ) {}

  play = (bot: BotInfo) => {
    this.dialogBot = undefined;
    this.start(bot);
  };
}
