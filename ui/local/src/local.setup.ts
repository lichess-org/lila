import { SetupDialog } from './setupDialog';
import { BotCtrl } from './botCtrl';
import { AssetDb } from './assetDb';

interface LocalModalOpts {}

export default async function initModule(opts: LocalModalOpts): Promise<SetupDialog> {
  opts;
  return new SetupDialog(await new BotCtrl(new AssetDb()).initLibots());
}
