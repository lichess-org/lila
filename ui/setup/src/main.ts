import { SetupCtrl } from './ctrl';
import { ParentCtrl } from './interfaces';

export async function initModule(root: ParentCtrl) {
  await site.dialog.ready;
  return new SetupCtrl(root);
}
