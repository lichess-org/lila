import * as control from './control';
import type AnalyseCtrl from './ctrl';

export default function (ctrl: AnalyseCtrl) {
  return {
    playUci: ctrl.playUci,
    navigate: {
      next: () => control.next(ctrl),
      prev: () => control.prev(ctrl),
      first: () => control.first(ctrl),
      last: () => control.last(ctrl),
    },
  };
}
