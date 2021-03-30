import { VNode } from 'snabbdom/vnode';
import { Run } from '../interfaces';
declare type OnFlag = () => void;
export default function renderClock(run: Run, onFlag: OnFlag, withBonus: boolean): VNode;
export {};
