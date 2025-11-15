import { attributesModule, classModule, init } from 'snabbdom';
import { GameCtrl } from './gameCtrl';
import { DevCtrl } from './devCtrl';
import { DevAssets, type AssetList } from './devAssets';
import { renderDevSide } from './devSideView';
import { DevBotCtrl } from './devBotCtrl';
import { PushCtrl } from './pushCtrl';
import { env, makeEnv } from './devEnv';
import { renderGameView } from './gameView';
import { LocalDb } from './localDb';
import type { RoundController } from 'round';
import type { LocalPlayOpts, LocalSetup } from 'lib/bot/types';
import { makeZerofish } from 'lib/bot/botLoader';

const patch = init([classModule, attributesModule]);

type SetupOpts = LocalSetup & { id?: string; go?: true };

interface LocalPlayDevOpts extends LocalPlayOpts {
  assets?: AssetList;
  pgn?: string;
  name?: string;
  canPost: boolean;
}

export async function initModule(opts: LocalPlayDevOpts): Promise<void> {
  if (opts.pgn && opts.name) {
    makeEnv({ bot: new DevBotCtrl(), assets: new DevAssets() });
    await Promise.all([env.bot.init(), env.assets.init()]);
    await env.assets.importPgn(
      opts.name,
      new Blob([opts.pgn], { type: 'application/x-chess-pgn' }),
      16,
      true,
    );
    return;
  }
  if (window.screen.width < 1260) return;
  makeEnv({
    redraw,
    bot: new DevBotCtrl(
      await makeZerofish({
        locator: (file: string) => site.asset.url(`npm/${file}`, { documentOrigin: file.endsWith('js') }),
        dev: true,
      }),
    ),
    push: new PushCtrl(),
    assets: new DevAssets(opts.assets),
    dev: new DevCtrl(),
    db: new LocalDb(),
    game: new GameCtrl(opts),
    canPost: opts.canPost,
  });

  await Promise.all([env.db.init(), env.bot.init(opts.bots), env.dev.init(), env.assets.init()]);
  const hash = hashOpts();
  env.game.load({
    ...JSON.parse(localStorage.getItem('botdev.setup') ?? '{}'),
    ...(hash.id || !Object.keys(hash).length ? await env.db.get(hash.id) : hash),
  });

  const el = document.querySelector('main') ?? document.createElement('main');
  document.getElementById('main-wrap')?.appendChild(el);

  let vnode = patch(el, renderGameView(renderDevSide()));

  env.round = await site.asset.loadEsm<RoundController>('round', { init: env.game.proxy.roundOpts });
  redraw();

  function redraw() {
    vnode = patch(vnode, renderGameView(renderDevSide()));
    env.round.redraw();
  }
}

function hashOpts(): SetupOpts {
  const params = location.hash
    .slice(1)
    .split('&')
    .map(p => decodeURIComponent(p).split('='))
    .filter(p => p.length === 2);
  const opts = Object.fromEntries(params);
  if ('initial' in opts) opts.initial = Number(opts.initial);
  if ('increment' in opts) opts.increment = Number(opts.increment);
  return opts;
}
