import { botAssetUrl } from 'local/assets';
import { BotOpts } from './interfaces';

import { looseH as h } from 'common/snabbdom';
import { BotInfo } from 'local';

export class BotPlayCtrl {
  constructor(
    readonly opts: BotOpts,
    readonly redraw: () => void,
  ) {
    console.log('BotPlayCtrl', opts);
  }

  view = () => h('main.bot-play-app', [this.viewBotList()]);

  private viewBotList = () => h('div.bpa__setup__bots', this.opts.bots.map(this.viewBotCard));

  private viewBotCard = (bot: BotInfo) =>
    h('div.bpa__bot-card', [
      h('img.bpa__bot-card__image', { attrs: { src: bot?.image && botAssetUrl('image', bot.image) } }),
      h('div.bpa__bot-card__content', [
        h('h2.bpa__bot-card__name', bot.name),
        h('p.bpa__bot-card__description', bot.description),
      ]),
    ]);
}
