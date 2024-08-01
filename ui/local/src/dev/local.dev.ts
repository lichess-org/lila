import { attributesModule, classModule, init } from 'snabbdom';
import { GameCtrl } from '../gameCtrl';
import { DevCtrl } from './devCtrl';
import { DevRepo, AssetList } from './devRepo';
import { renderDevView } from './devView';
import { BotCtrl } from '../botCtrl';
import { ShareCtrl } from './shareCtrl';
import renderGameView from '../gameView';
import type { RoundController } from 'round';
import type { LocalPlayOpts } from '../types';

const patch = init([classModule, attributesModule]);

interface LocalPlayDevOpts extends LocalPlayOpts {
  assets: AssetList;
}

export async function initModule(opts: LocalPlayDevOpts): Promise<void> {
  const devRepo = new DevRepo(opts.assets);
  const botCtrl = new BotCtrl(devRepo);
  devRepo.share = new ShareCtrl(botCtrl);
  await botCtrl.init(opts.bots);

  if (localStorage.getItem('local.setup')) {
    if (!opts.setup) opts.setup = JSON.parse(localStorage.getItem('local.setup')!);
  }
  if (opts.setup) {
    botCtrl.whiteUid = opts.setup.white;
    botCtrl.blackUid = opts.setup.black;
  }

  const gameCtrl = new GameCtrl(opts, botCtrl, redraw);
  const devCtrl = new DevCtrl(gameCtrl, redraw);

  const el = document.createElement('main');
  document.getElementById('main-wrap')?.appendChild(el);

  let vnode = patch(el, renderGameView(gameCtrl, renderDevView(devCtrl)));

  gameCtrl.round = await site.asset.loadEsm<RoundController>('round', { init: gameCtrl.roundOpts });
  redraw();

  function redraw() {
    vnode = patch(vnode, renderGameView(gameCtrl, renderDevView(devCtrl)));
    gameCtrl.round.redraw();
  }
}
