import { VNode } from 'snabbdom/vnode';
import { Config, Run } from '../interfaces';
export declare const playModifiers: (run: Run) => {
    'puz-mod-puzzle': boolean;
    'puz-mod-move': boolean;
    'puz-mod-malus-slow': boolean;
    'puz-mod-bonus-slow': boolean;
};
export declare const renderCombo: (config: Config, renderBonus: (bonus: number) => string) => (run: Run) => VNode;
