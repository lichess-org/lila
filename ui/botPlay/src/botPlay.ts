import { BotOpts } from './interfaces';

import { looseH as h } from 'common/snabbdom';

export class BotPlay {
  constructor(
    readonly opts: BotOpts,
    readonly redraw: () => void,
  ) {}

  view = () => h('main.bot-play-app', 'bot play app');
}
