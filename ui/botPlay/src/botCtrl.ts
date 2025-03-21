import SetupCtrl from './setup/setupCtrl';
import PlayCtrl from './play/playCtrl';
import { BotOpts, LocalBridge } from './interfaces';
import { BotInfo } from 'local';
import { BotCtrl as LocalBotCtrl } from 'local/botCtrl';
import { setupView } from './setup/setupView';
import { playView } from './play/view/playView';
import { storedJsonProp } from 'common/storage';
import { alert } from 'common/dialogs';
import { Bot } from 'local/bot';
import { Assets } from 'local/assets';
import { opposite } from 'chessops';
import { Game, makeGame } from './game';
import { debugCli } from './debug';
import { pubsub } from 'common/pubsub';

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

  private newGame = (bot: BotInfo, pov: Color) => this.resumeGameAndRedraw(makeGame(bot.uid, pov));

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
      bridge: this.makeLocalBridge(bot, opposite(game.pov)),
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

  private makeLocalBridge = async (info: BotInfo, color: Color): Promise<LocalBridge> => {
    const bots = new LocalBotCtrl();
    const assets = new Assets(bots);
    const uids = { [opposite(color)]: undefined, [color]: info.uid };
    bots.setUids(uids);
    bots.reset();
    await bots.init(this.opts.bots);
    const bot = new Bot(info);

    return {
      move: args => bot.move({ ...args, bots, assets }),
      playSound: (c, eventList) => bots.playSound(c, eventList, assets),
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
