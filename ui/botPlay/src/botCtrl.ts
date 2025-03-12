import { SetupCtrl } from './setup/setupCtrl';
import { PlayCtrl } from './play/playCtrl';
import { BotOpts } from './interfaces';
import { BotInfo } from 'local';

export class BotCtrl {
  setupCtrl: SetupCtrl;
  playCtrl?: PlayCtrl;

  constructor(
    readonly opts: BotOpts,
    readonly redraw: () => void,
  ) {
    this.setupCtrl = new SetupCtrl(opts, this.start, redraw);
    // debug, reach the game screen immediately:
    setTimeout(() => this.start(opts.bots[0]), 100);
  }

  private start = (bot: BotInfo) => {
    const game = { opponent: bot, moves: [] };
    this.playCtrl = new PlayCtrl(game, this.redraw);
    this.redraw();
  };

  view = () => (this.playCtrl ? this.playCtrl.view() : this.setupCtrl.view());
}
