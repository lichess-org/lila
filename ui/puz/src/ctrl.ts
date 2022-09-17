import { PuzVm } from './interfaces';
import { Run } from './interfaces';

export class PuzCtrl {
  run: Run;
  vm: PuzVm;
  toggleFilterSlow: () => void;
  toggleFilterFailed: () => void;
  trans: Trans;
}
