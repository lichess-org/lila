import type AnalyseCtrl from './ctrl';

export default function (ctrl: AnalyseCtrl) {
  return {
    playUci: ctrl.playUci,
    navigate: ctrl.navigate,
  };
}
