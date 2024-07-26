import { SetupDialog } from './setupDialog';
import { BotCtrl } from './botCtrl';
import { AssetDb } from './assetDb';
import type { LocalSetup } from './types';

export default async function initModule(opts: LocalSetup): Promise<SetupDialog> {
  if (localStorage.getItem('local.setup')) {
    if (!opts) opts = JSON.parse(localStorage.getItem('local.setup')!);
  }
  return new SetupDialog(await new BotCtrl(new AssetDb()).initLibots(), opts);
}
