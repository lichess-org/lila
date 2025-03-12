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
  }

  private start = (bot: BotInfo) => {
    this.playCtrl = new PlayCtrl({ opponent: bot, moves: [] }, this.redraw);
    this.redraw();
  };

  view = () => (this.playCtrl ? this.playCtrl.view() : this.setupCtrl.view());
}
