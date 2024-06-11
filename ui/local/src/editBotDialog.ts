import type { Libot, Libots, BotInfo, Mapping } from './types';
import { ZerofishBot, ZerofishBots } from './zerofishBot';
import { BotCtrl } from './botCtrl';
import { HandOfCards } from './handOfCards';
import { defined, escapeHtml } from 'common';
import { GameCtrl } from './gameCtrl';
import { settingHtml } from './editSetting';
import { renderMapping } from './mappingView';
import * as licon from 'common/licon';

interface ZerofishBotEditor extends ZerofishBot {
  [key: string]: any;
}

interface BotInfoReader extends BotInfo {
  readonly [key: string]: any;
}

interface ValueElement extends HTMLElement {
  value: string;
}

export class EditBotDialog {
  view: HTMLElement;
  hand: HandOfCards;
  bots: ZerofishBots;
  uid: string;
  botEl: HTMLElement;
  dlg: Dialog;

  constructor(
    readonly botCtrl: BotCtrl,
    readonly gameCtrl: GameCtrl,
    readonly color: Color,
    readonly setBot: (uid: string) => void,
  ) {
    this.bots = botCtrl.zerofishBots;
    this.view = $as<HTMLElement>(`<div class="with-hand-of-cards"><div class="edit-bot"></div></div>`);
    this.selectBot();
    this.hand = new HandOfCards({
      view: () => this.view,
      drops: () => [this.botEl],
      cardData: () =>
        Object.values(this.bots)
          .map(b => b.card)
          .filter(defined),
      select: (_: HTMLElement, cardId?: string) => this.selectBot(`#${cardId}`),
      autoResize: false,
    });
  }

  show() {
    return new Promise<void>(resolve => {
      site.dialog
        .dom({
          css: [{ hashed: 'local.test.setup' }],
          htmlText: `<div class="chin">
            <button class="button button-empty bot-json-one">Bot JSON</button>
            <button class="button button-empty bot-json-all">All JSON</button>
            <button class="button button-empty button-red bot-unrate-one">Unrate</button>
            <button class="button button-empty button-red bot-unrate-all">Unrate All</button>
            <button class="button button-empty button-red bot-clear-one">Clear</button>
            <button class="button button-empty button-red bot-clear-all">Clear All</button>
          </div>`,
          append: [{ node: this.view, where: '.chin', how: 'before' }],
          action: [
            {
              selector: '[data-type]',
              event: ['input', 'change'],
              result: (_, __, e) => {
                const setting = e.target as ValueElement;
                const id = setting.parentElement!.id;
                this.setEnabled(setting, true);
                if (setting.dataset.type === 'number') this.setProperty(id, +setting.value);
                else if (setting.dataset.type === 'string') this.setProperty(id, setting.value);
                else this.setProperty(id, 'GO BUTTER!');
                this.bot.update();
              },
            },
            { selector: '.toggle-enabled', event: 'change', result: this.radioToggle },
            { selector: '.hasPanel', result: this.showPanel },
            { selector: '.bot-json-one', result: () => this.showJson([this.bot.uid]) },
            { selector: '.bot-json-all', result: () => this.showJson() },
            { selector: '.bot-unrate-one', result: () => this.clearRatings([this.bot.uid]) },
            { selector: '.bot-unrate-all', result: () => this.clearRatings() },
            { selector: '.bot-clear-one', result: () => this.clearBots([this.bot.uid]) },
            { selector: '.bot-clear-all', result: () => this.clearBots() },
          ],
        })
        .then(async dlg => {
          this.dlg = dlg;
          this.updateView();
          const allDone = dlg.show();
          window.addEventListener('resize', this.resize);
          this.resize();
          await allDone;
          window.removeEventListener('resize', this.resize);
          resolve();
        });
    });
  }

  resize = () => {
    this.hand.resize();
  };

  showPanel = (dlg: Dialog, action: Action, e: Event) => {
    let div = e.target as HTMLElement;
    while (!div.id) div = div.parentElement!;
    $('.hasPanel', this.view).each((_, el) =>
      el === div ? el.classList.toggle('selected') : el.classList.remove('selected'),
    );
    const showing = div.classList.contains('selected');
    const editPanel = this.view.querySelector('.edit-panel') as HTMLElement;
    if (!showing) {
      editPanel.dataset.panelShowing = '';
      editPanel.innerHTML = '';
      return;
    }
    if (editPanel.dataset.panelShowing === div.id) return;
    const mapping = (this.getProperty(div.id) as Mapping) ?? this.getDefault(div.id);
    if (!('data' in mapping && 'scale' in mapping)) throw new Error(`error on ${div.id}`);
    editPanel.dataset.panelShowing = div.id;
    const canvas = $as<HTMLCanvasElement>(`<canvas id="poop"></canvas>`);
    editPanel.innerHTML = '';
    editPanel.appendChild(canvas);
    renderMapping(canvas, mapping);
  };

  get bot(): ZerofishBotEditor {
    return this.bots[this.uid];
  }

  get default(): BotInfoReader {
    return this.botCtrl.default;
  }

  get selected(): string[] {
    return this.view.querySelector('.hasPanel.selected')?.id.split('_') ?? [];
  }

  updateView = (force: boolean = false) => {
    if (force) {
      this.view.querySelector('.edit-bot')!.outerHTML = this.editViewHtml();
      this.dlg?.refresh();
    }
    //renderMapping(this.view.querySelector('.edit-panel canvas') as HTMLCanvasElement, this.bot.mix!);
    if (force) this.hand.resize();
  };

  selectBot(uid = this.botCtrl[this.color]!.uid) {
    this.uid = uid;
    this.view.style.setProperty(`---${this.color}-image-url`, `url(${this.bot.imageUrl})`);
    this.view.querySelector('.edit-bot')!.outerHTML = this.editViewHtml();
    this.view.querySelector(`.${this.color} .placard`)!.textContent = this.bot.description;
    this.botEl = this.view.querySelector('.player') as HTMLElement;
    this.dlg?.refresh();
    this.setBot(this.uid);
  }

  setProperty = (id: string, value?: string | number) => {
    const path = id.split('_');
    console.log(this.bot, path, value);
    if (value === undefined) removePath({ obj: this.bot, path });
    else setPath({ obj: this.bot, path, value: value });
  };

  getProperty = (id: string) => {
    const path = id.split('_');
    return path.reduce((obj, key) => {
      return obj[key];
    }, this.bot);
  };

  getDefault = (id: string) => {
    const path = id.split('_');
    return structuredClone(
      path.reduce((obj, key) => {
        return obj[key];
      }, this.default),
    );
  };

  editViewHtml() {
    return `<div class="edit-bot">
        <div class="player ${this.color}"><div class="placard ${this.color}">Player</div></div>
        <div class="name">
          ${settingHtml({
            label: 'Name',
            id: 'name',
            type: 'text',
            value: this.bot.name,
            required: true,
          })}
        </div>
        <div class="description">
          ${settingHtml({
            label: 'Description',
            id: 'description',
            type: 'textarea',
            value: this.bot.description,
            required: true,
          })}
        </div>
        <div class="settings">
          ${settingHtml({
            label: 'book',
            id: 'book_lifat',
            type: 'select',
            value: this.bot.book?.lifat ?? '',
            choices: lifatBooks,
          })}
          <fieldset class="zero"><legend>Lc0</legend>${this.zeroHtml()}</fieldset>
          <fieldset class="fish"><legend>Stockfish</legend>${this.fishHtml()}</fieldset>
          <fieldset class="mix"><legend>Search Mix</legend>${this.searchMixHtml()}</fieldset>
        </div>
        <div class="edit-panel"></div>
      </div>`;
  }

  searchMixHtml() {
    return `${settingHtml({
      label: 'search mix',
      id: 'searchMix',
      type: 'range',
      value: String(this.bot.mix ?? 0),
      min: 0,
      max: 1,
      step: 0.01,
      hasPanel: true,
    })}`;
  }

  zeroHtml() {
    const maxDepth = (this.bot.zero && lifatNets[this.bot.zero.netName]) || 0;
    return `${settingHtml({
      label: 'model',
      id: 'zero_netName',
      type: 'select',
      value: this.bot.zero?.netName ?? 'maia-1100.pb',
      choices: ['none', ...Object.keys(lifatNets)],
    })}
    ${maxDepth > 0 ? this.searchHtml('zero') : ''}`;
  }

  fishHtml() {
    return `${settingHtml({
      label: 'multipv',
      id: 'fish_multipv',
      type: 'number',
      value: String(this.bot.fish?.multipv ?? ''),
      min: 1,
      max: 50,
    })}
    ${this.searchHtml('fish')}`;
  }

  searchHtml(engine: 'zero' | 'fish') {
    return `${settingHtml({
      label: 'depth',
      id: `${engine}_search_depth`,
      type: 'number',
      value: String(this.bot[engine]?.search?.depth ?? ''),
      min: 1,
      max: engine === 'fish' ? 24 : 3,
      radioGroup: `${engine}_search`,
    })}
    ${settingHtml({
      label: 'nodes',
      id: `${engine}_search_nodes`,
      type: 'number',
      value: String(this.bot[engine]?.search?.nodes ?? ''),
      min: 1,
      max: 1000000,
      radioGroup: `${engine}_search`,
    })}
    ${settingHtml({
      label: 'movetime',
      id: `${engine}_search_movetime`,
      type: 'number',
      value: String(this.bot[engine]?.search?.movetime ?? ''),
      min: 1,
      max: 10000,
      radioGroup: `${engine}_search`,
    })}`;
  }

  radioToggle = (dlg: Dialog, action: Action, e: Event) => {
    const cbox = e.target as HTMLInputElement;
    this.setEnabled(cbox.nextElementSibling as HTMLInputElement, cbox.checked);
    this.bot.update();
  };

  isEnabled = (input: HTMLInputElement) => {
    return input.parentElement!.classList.contains('disabled') ? undefined : input.value;
  };

  setEnabled = (input: ValueElement, enabled: boolean) => {
    const setting = input.parentElement!;
    const settings = setting.dataset.radioGroup
      ? [...this.view.querySelectorAll(`[data-radio-group="${setting.dataset.radioGroup}"]`)]
      : [setting];
    settings.forEach((div: HTMLDivElement) => {
      const toggle = div.querySelector('.toggle-enabled') as HTMLInputElement;
      toggle.checked = div === setting && enabled;
      div.classList.toggle('disabled', !enabled);
      const input = div.querySelector('[data-type]') as ValueElement;
      this.setProperty(div.id, toggle.checked ? input.value : undefined);
    });
    this.bot.update();
  };

  clearBots = async (uids?: string[]) => {
    await this.botCtrl.clearLocalBots(uids);
    this.bots = this.botCtrl.zerofishBots;
    this.selectBot(this.bot.uid in this.bots ? this.bot.uid : Object.keys(this.bots)[0]);
    alert(uids ? `Cleared ${uids.join(' ')}` : 'Local bots cleared');
  };

  clearRatings = (uids?: string[]) => {
    const unrate: Libot[] = [];
    if (uids) uids.forEach(uid => unrate.push(this.bots[uid]));
    else Object.values(this.bots).forEach(bot => unrate.push(bot));
    unrate.filter(defined).forEach(bot => (bot.glicko = undefined));
    this.gameCtrl.redraw();
  };

  showJson = async (uids?: string[]) => {
    const text = escapeHtml(
      JSON.stringify(uids ? uids.map(id => this.bots[id]) : [...Object.values(this.bots)], null, 2),
    );
    const clear = text ? `<button class="button button-empty button-red clear">clear local</button>` : '';
    const copy = `<button class="button copy" data-icon="${licon.Clipboard}"> copy</button>`;
    const dlg = await site.dialog.dom({
      class: 'diagnostic',
      css: [{ hashed: 'bits.diagnosticDialog' }],
      htmlText: `
      <h2>bots.json</h2>
      <pre tabindex="0" class="json">${text}</pre>
      <span class="actions">${clear}<div class="spacer"></div>${copy}</span>`,
    });
    const select = () =>
      setTimeout(() => {
        const range = document.createRange();
        range.selectNodeContents(dlg.view.querySelector('.json')!);
        window.getSelection()?.removeAllRanges();
        window.getSelection()?.addRange(range);
      }, 0);
    $('.json', dlg.view).on('focus', select);
    $('.copy', dlg.view).on('click', () =>
      navigator.clipboard.writeText(text).then(() => {
        const copied = $(`<div data-icon="${licon.Checkmark}" class="good"> COPIED</div>`);
        $('.copy', dlg.view).before(copied);
        setTimeout(() => copied.remove(), 2000);
      }),
    );
    dlg.showModal();
  };
}

function removePath({ obj, path }: { obj: any; path: string[] }) {
  if (!obj) return;
  if (path.length > 1) removePath({ obj: obj[path[0]], path: path.slice(1) });
  if (typeof obj[path[0]] !== 'object' || Object.keys(obj[path[0]].length === 0)) delete obj[path[0]];
}

function setPath({ obj, path, value }: { obj: any; path: string[]; value: any }) {
  if (path.length === 0) return;
  if (path.length === 1) obj[path[0]] = value;
  else if (!(path[0] in obj)) obj[path[0]] = {};
  setPath({ obj: obj[path[0]], path: path.slice(1), value });
}

const lifatNets: { [name: string]: number | undefined } = {
  'badgyal-8.pb': 3,
  'evilgyal-6.pb': 3,
  'goodgyal-5.pb': 3,
  'tinygyal-8.pb': 4,
  'naise700.pb': undefined,
  'nocap2000.pb': undefined,
  'maia-1100.pb': undefined,
  'maia-1200.pb': undefined,
  'maia-1300.pb': undefined,
  'maia-1400.pb': undefined,
  'maia-1500.pb': undefined,
  'maia-1600.pb': undefined,
  'maia-1700.pb': undefined,
  'maia-1800.pb': undefined,
  'maia-1900.pb': undefined,
};

const lifatBooks = [
  'Book.bin',
  'codekiddy.bin',
  'DCbook_large.bin',
  'Elo2400.bin',
  'final-book.bin',
  'gavibook.bin',
  'gavibook-small.bin',
  'gm2600.bin',
  'komodo.bin',
  'KomodoVariety.bin',
  'Performance.bin',
  'varied.bin',
];
