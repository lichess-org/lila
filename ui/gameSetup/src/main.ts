import { SetupCtrl } from './ctrl';
import { ParentCtrl } from './interfaces';

export function initModule(root: ParentCtrl): SetupCtrl {
  return new SetupCtrl(root);
}
