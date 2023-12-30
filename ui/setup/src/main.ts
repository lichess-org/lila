import { SetupCtrl } from './ctrl';
import { ParentCtrl } from './interfaces';
import { ready as loadDialogPolyfill } from 'common/dialog';

export async function initModule(root: ParentCtrl) {
  await loadDialogPolyfill;
  return new SetupCtrl(root);
}
