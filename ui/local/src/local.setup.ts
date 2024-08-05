import { showSetupDialog } from './setupDialog';
import { BotCtrl } from './botCtrl';
import { AssetDb } from './assetDb';
import type { LocalSetupOpts } from './types';

export default async function initModule(opts: LocalSetupOpts = {}): Promise<void> {
  if (localStorage.getItem('local.setup')) {
    opts = { ...opts, ...JSON.parse(localStorage.getItem('local.setup')!) };
  }
  opts.bots ??= (await fetch('/local/list').then(res => res.json())).bots;
  showSetupDialog(await new BotCtrl(new AssetDb()).initLibots(opts.bots!), opts);
}
