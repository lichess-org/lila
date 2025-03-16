import SetupCtrl from './setup/setupCtrl';
import PlayCtrl from './play/playCtrl';
import { BotOpts, Game } from './interfaces';
import { BotInfo } from 'local';
import { playView } from './play/playView';
import { setupView } from './setup/setupView';
import { storedJsonProp } from 'common/storage';
import { alert } from 'common/dialogs';

export class BotCtrl {
  setupCtrl: SetupCtrl;
  playCtrl?: PlayCtrl;

  currentGame = storedJsonProp<Game | null>('bot.current-game', () => null);

  constructor(
    readonly opts: BotOpts,
    readonly redraw: () => void,
  ) {
    this.setupCtrl = new SetupCtrl(opts, this.newGame, redraw);
    this.loadSavedGame();
  }

  private loadSavedGame = () => {
    const game = this.currentGame();
    if (game) this.resumeGame(game);
  };

  private newGame = (bot: BotInfo) => {
    const game: Game = { botId: bot.uid, sans: [], pov: 'white' };
    this.resumeGame(game);
    this.redraw();
  };

  private resumeGame = (game: Game) => {
    const bot = this.opts.bots.find(b => b.uid === game.botId);
    if (!bot) {
      alert(`Couldn't find your opponent ${game.botId}`);
      return;
    }
    this.playCtrl = new PlayCtrl({
      pref: this.opts.pref,
      game,
      bot,
      redraw: this.redraw,
      save: g => this.currentGame(g),
      close: this.closeGame,
    });
  };

  private closeGame = () => {
    this.playCtrl = undefined;
    this.redraw();
  };

  view = () => (this.playCtrl ? playView(this.playCtrl) : setupView(this.setupCtrl));
}
