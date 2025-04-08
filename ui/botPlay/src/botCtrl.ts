import SetupCtrl from './setup/setupCtrl';
import PlayCtrl from './play/playCtrl';
import type { BotOpts, LocalBridge } from './interfaces';
import { type BotInfo } from 'lib/bot/types';
import { BotLoader } from 'lib/bot/botLoader';
import { setupView } from './setup/setupView';
import { playView } from './play/view/playView';
import { storedJsonProp } from 'lib/storage';
import { alert } from 'lib/view/dialogs';
import { opposite } from 'chessops';
import { type Game, makeGame } from './game';
import { debugCli } from './debug';
import { pubsub } from 'lib/pubsub';

export class BotCtrl {
  setupCtrl: SetupCtrl;
  playCtrl?: PlayCtrl;

  currentGame = storedJsonProp<Game | null>('bot.current-game', () => null);

  constructor(
    readonly opts: BotOpts,
    readonly redraw: () => void,
  ) {
    this.setupCtrl = new SetupCtrl(opts, this.currentGame, this.resume, this.newGame, redraw);
    debugCli(this.resumeGameAndRedraw);
    addZenSupport();

    this.resume(); // auto-join the ongoing game
  }

  private resume = () => {
    const game = this.currentGame();
    if (game) this.resumeGame(game);
  };

  private newGame = (bot: BotInfo, pov: Color) =>
    this.resumeGameAndRedraw(
      makeGame(bot.uid, pov, {
        initial: 300,
        increment: 2,
        moretime: 0,
      }),
    );

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
      bridge: this.makeLocalBridge(bot),
      redraw: this.redraw,
      save: g => this.currentGame(g),
      close: this.closeGame,
      rematch: () => this.newGame(bot, opposite(game.pov)),
    });
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
