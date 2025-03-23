import { showSetupDialog } from './setupDialog';
import { DevBotCtrl } from './devBotCtrl';
import { DevAssets } from './devAssets';
import { env, makeEnv } from './devEnv';
import type { LocalSetup } from '../types';

export default async function initModule(opts: LocalSetup = {}): Promise<void> {
  opts = { ...JSON.parse(localStorage.getItem('local.setup') ?? '{}'), ...opts };
  opts.initial ??= Infinity;
  makeEnv({
    redraw: () => {},
    bot: await new DevBotCtrl().init(),
    assets: new DevAssets(),
  });
  await env.assets.init();
  showSetupDialog(opts);
}
