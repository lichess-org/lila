import { botAssetUrl } from 'local/assets';
import { BotOpts } from '../interfaces';
import { bind, looseH as h } from 'common/snabbdom';
import { snabDialog } from 'common/dialog';
import { BotInfo } from 'local';

export class SetupCtrl {
  dialogBot: BotInfo | undefined;

  constructor(
    readonly opts: BotOpts,
    readonly start: (bot: BotInfo) => void,
    readonly redraw: () => void,
  ) {
    console.log('BotPlayCtrl', opts);
  }

  view = () => h('main.bot-app.bot-setup', [this.viewBotList(), this.viewSetupDialog()]);

  private viewBotList = () => h('div.bot-setup__bots', this.opts.bots.map(this.viewBotCard));

  private viewBotCard = (bot: BotInfo) =>
    h(
      'div.bot-setup__bot-card',
      {
        hook: bind('click', () => {
          this.dialogBot = bot;
          this.redraw();
        }),
      },
      [
        h('img.bot-setup__bot-card__image', {
          attrs: { src: bot?.image && botAssetUrl('image', bot.image) },
        }),
        h('div.bot-setup__bot-card__content', [
          h('h2.bot-setup__bot-card__name', bot.name),
          h('p.bot-setup__bot-card__description', bot.description),
        ]),
      ],
    );

  private viewSetupDialog = () => {
    const bot = this.dialogBot;
    if (!bot) return undefined;
    return snabDialog({
      onClose: () => {
        this.dialogBot = undefined;
        this.redraw();
      },
      modal: true,
      noClickAway: true,
      vnodes: [
        h('h2', bot.name),
        h(
          'button.button.button-fat',
          {
            hook: bind('click', () => this.start(bot)),
          },
          'Play',
        ),
      ],
    });
  };
}
