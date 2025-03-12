import { Game } from '../interfaces';
import { looseH as h } from 'common/snabbdom';

export class PlayCtrl {
  constructor(
    readonly game: Game,
    readonly redraw: () => void,
  ) {}

  view = () => h('div', 'now playing!');
}
