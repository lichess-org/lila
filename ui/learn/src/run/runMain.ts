import { LearnOpts } from '../main';
import runCtrl from './runCtrl';
import runView from './runView';

export default function (opts: LearnOpts, trans: Trans) {
  return {
    controller: function () {
      return runCtrl(opts, trans);
    },
    view: runView,
  };
}
