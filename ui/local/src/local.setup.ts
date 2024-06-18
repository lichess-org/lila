import { LocalDialog } from './setupDialog';
import { BotCtrl } from './botCtrl';
import { AssetDb } from './assetDb';
import { Libots } from './types';

interface LocalModalOpts {}

export default async function initModule(opts: LocalModalOpts) {
  opts;
  return new LocalDialog(await new BotCtrl().initLibots());
}
