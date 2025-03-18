import SetupCtrl from './setup/setupCtrl';
import PlayCtrl from './play/playCtrl';
import { BotOpts } from './interfaces';
import { BotInfo, MoveArgs, MoveSource } from 'local';
import { BotCtrl as LocalBotCtrl } from 'local/botCtrl';
import { setupView } from './setup/setupView';
import { playView } from './play/view/playView';
import { storedJsonProp } from 'common/storage';
import { alert } from 'common/dialogs';
import { Bot } from 'local/bot';
import { Assets } from 'local/assets';
import makeZerofish from 'zerofish';
import { opposite } from 'chessops';
import { Game, makeGame } from './game';
import { debugCli } from './debug';

export class BotCtrl {
  setupCtrl: SetupCtrl;
  playCtrl?: PlayCtrl;

  currentGame = storedJsonProp<Game | null>('bot.current-game', () => null);

  constructor(
    readonly opts: BotOpts,
    readonly redraw: () => void,
  ) {
    this.setupCtrl = new SetupCtrl(opts, this.currentGame, this.resume, this.newGame, redraw);
    debugCli(this.resumeGame);
  }

  private resume = () => {
    const game = this.currentGame();
    console.log('resume', game);
    if (game) this.resumeGame(game);
  };

  private newGame = (bot: BotInfo, pov: Color) => {
    this.resumeGame(makeGame(bot.uid, pov));
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
      moveSource: this.makeLocalSource(bot, opposite(game.pov)),
      redraw: this.redraw,
      save: g => this.currentGame(g),
      close: this.closeGame,
      rematch: () => this.newGame(bot, opposite(game.pov)),
    });
    this.redraw();
  };

  private closeGame = () => {
    this.playCtrl = undefined;
    this.redraw();
  };

  view = () => (this.playCtrl ? playView(this.playCtrl) : setupView(this.setupCtrl));

  private makeLocalSource = async (info: BotInfo, color: Color): Promise<MoveSource> => {
    const zerofish = await makeZerofish({
      locator: (file: string) => site.asset.url(`npm/${file}`, { documentOrigin: file.endsWith('js') }),
      nonce: document.body.dataset.nonce,
    });
    const bots = new LocalBotCtrl(zerofish);
    const assets = new Assets();
    const uids = { [opposite(color)]: undefined, [color]: info.uid };
    bots.setUids(uids);
    await Promise.all([bots.init(this.opts.bots), assets.init(bots)]);
    const bot = new Bot(info);

    return {
      move: (args: MoveArgs) => bot.move({ ...args, bots, assets }),
    };
  };
}
