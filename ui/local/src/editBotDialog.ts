import type { Libot, Libots } from './types';
import { ZerofishBot, ZerofishBots } from './zerofishBot';
import { BotCtrl } from './botCtrl';
import { HandOfCards } from './handOfCards';
import { defined } from 'common';
import { isTouchDevice } from 'common/device';
//import { clamp } from 'common';
import * as licon from 'common/licon';
//import { ratingView } from './components/ratingView';

const weights = [
  'badgyal-8.pb',
  'evilgyal-6.pb',
  'goodgyal-5.pb',
  'tinygyal-8.pb',
  'naise700.pb',
  'nocap2000.pb',
  'maia-1100.pb',
  'maia-1200.pb',
  'maia-1300.pb',
  'maia-1400.pb',
  'maia-1500.pb',
  'maia-1600.pb',
  'maia-1700.pb',
  'maia-1800.pb',
  'maia-1900.pb',
];

function labelId(label: string) {
  return label.toLowerCase().replace(/\s+/g, '-');
}

export class EditBotDialog {
  view: HTMLElement;
  botEl: HTMLElement;
  hand: HandOfCards;

  constructor(
    readonly bots: ZerofishBots,
    readonly color: Color,
    private uid: string,
  ) {
    this.view = $as<HTMLElement>(`<div class="with-hand-of-cards">
      <div class="edit-bot">
        <div class="player ${color}"><div class="placard ${color}">Player</div></div>
        <div class="name settings">
          ${this.settingHtml('Name', 'name', 'text', this.bot.name)}
        </div>
        <div class="description settings">
          ${this.settingHtml('Description', 'description', 'textarea', this.bot.description)}
        </div>
        <div class="col1 settings">
          <div class="zero">
            ${this.selectHtml('LC0 Model', 'lc0-model', this.bot.zero?.netName, weights)}
          </div>
          <div class="fish">
          </div>
          ${this.settingHtml('Frequency', 'freq', 'range', '0.5')}
        </div>
        <div class="col2 settings">
        </div>
      </div>
    </div>
    `);
    this.botEl = this.view.querySelector('.player')!;
    this.hand = new HandOfCards(
      this.view,
      [this.botEl],
      Object.values(this.bots)
        .map(b => b.card)
        .filter(defined),
      this.select,
    );
    this.botSelect(uid!);
    this.show();
  }

  get bot(): ZerofishBot {
    return this.bots[this.uid] as ZerofishBot;
  }
  set bot(bot: ZerofishBot) {
    this.uid = bot.uid;
  }
  settingHtml(label: string, id: string, type: string, value: string) {
    return `<div class="setting">
      <label for="${id}">${label}</label>
      ${
        type === 'textarea'
          ? `<textarea id="${id}">${value}</textarea>`
          : `<input id="${id}" type="${type}" value="${value}">`
      }
    </div>`;
  }
  selectHtml(label: string, id: string, value: string | undefined, choices: string[]) {
    if (!value) return '';
    return `<div class="setting">
      <label for="${id}">${label}</label>
      <select id="${id}">
        ${choices.map(c => `<option value="${c}"${c === value ? ' selected' : ''}>${c}</option>`).join('')}
      </select>
    </div>`;
  }
  botSelect(uid: string) {
    this.bot = this.bots[uid];
    this.view.style.setProperty(`---${this.color}-image-url`, `url(${this.bot.imageUrl})`);
    this.view.querySelector(`.${this.color} .placard`)!.textContent = this.bot.description;
  }

  show() {
    site.dialog
      .dom({
        //class: 'game-setup.local-setup',
        css: [{ hashed: 'local.test.setup' }],
        htmlText: `<div class="chin">
            <button class="button button-none" id="apply">Apply</button>
          </div>`,
        append: [{ node: this.view, where: '.chin', how: 'before' }],
        action: [
          {
            selector: '#lc0-model',
            event: 'change',
            result: (_, __, e) => {
              const model = (e.target as HTMLSelectElement).value;
              this.bot.zero = { ...this.bot.zero, netName: model };
            },
          },
          { selector: '#apply', result: this.apply },
        ],
      })
      .then(dlg => {
        dlg.showModal();
        this.hand.resize();
      });
  }
  apply = () => {
    this.bot.update();
  };
  select = (target: HTMLElement, domId?: string) => {
    this.botSelect(`#${domId}`);
  };
}
