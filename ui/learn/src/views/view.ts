import { VNode } from 'snabbdom';
import LearnCtrl from '../ctrl';
import home from './home';
import lesson from './lesson';

export default function (ctrl: LearnCtrl): VNode {
  if (!ctrl.vm) return home(ctrl);
  else return lesson(ctrl);
}
