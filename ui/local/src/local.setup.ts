import { SetupDialog } from './setupDialog';
import { BotCtrl } from './botCtrl';
import { AssetDb } from './assetDb';
import { Libots } from './types';

interface LocalModalOpts {}

export default async function initModule(opts: LocalModalOpts) {
  opts;
  return new SetupDialog(await new BotCtrl().initLibots());
}
