import { Assets } from 'local/assets';
import { BotOpts } from './interfaces';

import { looseH as h } from 'common/snabbdom';
import { BotInfo } from 'local';

export class BotPlayCtrl {
  constructor(
    readonly opts: BotOpts,
    readonly assets: Assets,
    readonly redraw: () => void,
  ) {
    console.log('BotPlayCtrl', opts);
  }

  view = () => h('main.bot-play-app', ['bot play app', this.viewBotList()]);

  private viewBotList = () => h('div.bpa__setup__bots', this.opts.bots.map(this.viewBotCard));

  private viewBotCard = (bot: BotInfo) =>
    h('div.bpa__bot-card', [bot.name, bot?.image && this.assets.getImageUrl(bot.image)]);
}
