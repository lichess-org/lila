import type { VNode } from 'snabbdom';
import type LearnCtrl from '../ctrl';
import home from './home';
import lesson from './lesson';

export default function (ctrl: LearnCtrl): VNode {
  if (!ctrl.vm) return home(ctrl);
  else return lesson(ctrl);
}
