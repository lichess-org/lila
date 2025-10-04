import SetupCtrl from './setup/setupCtrl';
import PlayCtrl from './play/playCtrl';
import type { BotOpts, LocalBridge } from './interfaces';
import { type BotInfo } from 'lib/bot/types';
import { BotLoader } from 'lib/bot/botLoader';
import { setupView } from './setup/view/setupView';
import { playView } from './play/view/playView';
import { alert } from 'lib/view/dialogs';
import { opposite } from 'chessops';
import { Game } from './game';
import { debugCli } from './debug';
import { pubsub } from 'lib/pubsub';
import { loadCurrentGame, saveCurrentGame } from './storage';
import type { ColorOrRandom } from 'lib/setup/interfaces';
import type { ClockConfig } from 'lib/game/clock/clockCtrl';

export class BotCtrl {
  setupCtrl: SetupCtrl;
  playCtrl?: PlayCtrl;

  constructor(
    readonly opts: BotOpts,
    readonly redraw: () => void,
  ) {
    this.setupCtrl = new SetupCtrl(opts, loadCurrentGame, this.resume, this.newGame, redraw);
    debugCli(this.resumeGameAndRedraw);
    addZenSupport();

    this.resume(); // auto-join the ongoing game
  }

  private resume = () => {
    const game = loadCurrentGame();
    if (game?.worthResuming()) this.resumeGame(game);
  };

  private newGame = (bot: BotInfo, pov: ColorOrRandom, clock?: ClockConfig) => {
    const color = pov == 'random' ? (Math.random() < 0.5 ? 'white' : 'black') : pov;
    this.resumeGameAndRedraw(new Game(bot.uid, color, clock));
  };

  private resumeGame = (game: Game) => {
    const bot = this.opts.bots.find(b => b.uid === game.botKey);
    if (!bot) {
      alert(`Couldn't find your opponent ${game.botKey}`);
      return;
    }
    try {
      this.playCtrl = new PlayCtrl({
        pref: this.opts.pref,
        game,
        bot,
        bridge: this.makeLocalBridge(bot),
        redraw: this.redraw,
        save: saveCurrentGame,
        close: this.closeGame,
        rematch: () => this.newGame(bot, opposite(game.pov), game.clockConfig),
      });
    } catch (e) {
      console.error('Failed to resume game', e);
      alert('Failed to resume game. Please start a new one.');
      saveCurrentGame(null);
      return;
    }
  };

  private resumeGameAndRedraw = (game: Game) => {
    this.resumeGame(game);
    this.redraw();
  };

  private closeGame = () => {
    this.playCtrl = undefined;
    this.redraw();
  };

  view = () => (this.playCtrl ? playView(this.playCtrl) : setupView(this.setupCtrl));

  private makeLocalBridge = async (info: BotInfo): Promise<LocalBridge> => {
    const loader = new BotLoader();

    await loader.init(this.opts.bots);
    await loader.preload(info.uid);

    const bot = loader.bots.get(info.uid)!;

    return {
      move: args => bot.move(args),
      playSound: eventList => bot.playSound(eventList),
    };
  };
}

const addZenSupport = () => {
  pubsub.on('zen', () => {
    $('body').toggleClass('zen');
    window.dispatchEvent(new Event('resize'));
  });
  $('#zentog').on('click', () => pubsub.emit('zen'));
};
