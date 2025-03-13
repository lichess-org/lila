import SetupCtrl from './setup/setupCtrl';
import PlayCtrl from './play/playCtrl';
import { BotOpts } from './interfaces';
import { BotInfo } from 'local';
import { playView } from './play/playView';

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
    this.playCtrl = new PlayCtrl(this.opts.pref, game, this.redraw);
    this.redraw();
  };

  view = () => (this.playCtrl ? playView(this.playCtrl) : this.setupCtrl.view());
}
