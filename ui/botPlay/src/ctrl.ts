import { botAssetUrl } from 'local/assets';
import { BotOpts } from './interfaces';
import { bind, looseH as h } from 'common/snabbdom';
import { snabDialog } from 'common/dialog';
import { BotInfo } from 'local';

export class BotPlayCtrl {
  dialogBot: BotInfo | undefined;

  constructor(
    readonly opts: BotOpts,
    readonly redraw: () => void,
  ) {
    console.log('BotPlayCtrl', opts);
  }

  view = () => h('main.bot-play-app', [this.viewBotList(), this.viewSetupDialog()]);

  private makeHref = (_bot: BotInfo) => `#`;

  private viewBotList = () => h('div.bpa__setup__bots', this.opts.bots.map(this.viewBotCard));

  private viewBotCard = (bot: BotInfo) =>
    h(
      'div.bpa__bot-card',
      {
        hook: bind('click', () => {
          this.dialogBot = bot;
          this.redraw();
        }),
      },
      [
        h('img.bpa__bot-card__image', { attrs: { src: bot?.image && botAssetUrl('image', bot.image) } }),
        h('div.bpa__bot-card__content', [
          h('h2.bpa__bot-card__name', bot.name),
          h('p.bpa__bot-card__description', bot.description),
        ]),
      ],
    );

  private viewSetupDialog = () =>
    this.dialogBot
      ? snabDialog({
          onClose: () => {
            this.dialogBot = undefined;
            this.redraw();
          },
          modal: true,
          noClickAway: true,
          vnodes: [
            h('h2', this.dialogBot.name),
            h(
              'a.button.button-fat',
              {
                attrs: {
                  href: this.makeHref(this.dialogBot),
                },
              },
              'Play',
            ),
          ],
        })
      : undefined;
}
