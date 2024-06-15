import type { Libot, Libots, BotInfo, Mapping } from './types';
import { ZerofishBot, ZerofishBots } from './zerofishBot';
import { BotCtrl } from './botCtrl';
import { HandOfCards } from './handOfCards';
import { defined, escapeHtml } from 'common';
import { GameCtrl } from './gameCtrl';
import { renderMapping } from './mappingView';
import { Settings } from './editBotSetting';
import { setSchemaBookChoices, setSchemaNetChoices, buildFromSchema } from './editBotSchema';
import * as licon from 'common/licon';

interface ZerofishBotEditor extends ZerofishBot {
  [key: string]: any;
}

export interface BotInfoReader extends BotInfo {
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
  settings: Settings;
  // radios: { [radioGroup: string]: string[] } = {};
  // requires: { [id: string]: string[] } = {};
  panel: HTMLElement;
  dlg: Dialog;

  constructor(
    readonly botCtrl: BotCtrl,
    readonly gameCtrl: GameCtrl,
    readonly color: Color,
    readonly setBot: (uid: string) => void,
  ) {
    setSchemaBookChoices(lifatBooks); // TODO - not here
    setSchemaNetChoices(lifatNets); // TODO - not here
    this.bots = botCtrl.zerofishBots;
    this.view = $as<HTMLElement>(`<div class="with-hand-of-cards"><div class="edit-bot"></div></div>`);
    this.selectBot();
    this.hand = new HandOfCards({
      view: () => this.view,
      drops: () => [{ el: this.playerEl, selected: this.bot.uid }],
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
            { selector: '[data-type]', event: ['input', 'change'], result: this.valueChange },
            { selector: '.toggle-enabled', event: 'change', result: this.toggleEnabled },
            //{ selector: '.has-panel', result: this.showPanel },
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

  makeEditView() {
    this.settings = new Settings();
    // this.radios = {};
    // this.requires = {};
    const el = $as<HTMLElement>(`<div class="edit-bot">`);
    const playerInfo = $as<HTMLElement>(`<div class="player-info">`);
    playerInfo.appendChild(buildFromSchema(this, ['bot_name']).div);
    const player = $as<HTMLElement>(`<div class="player ${this.color}">`);
    player.appendChild(buildFromSchema(this, ['bot_description']).div);
    playerInfo.appendChild(player);
    el.appendChild(playerInfo);
    el.appendChild(buildFromSchema(this, ['bot']).div);
    el.appendChild($as<HTMLElement>('<div class="edit-panel"><canvas></canvas></div>'));
    // for (const [id, deps] of Object.entries(this.requires)) {
    //   deps.forEach(dep => this.els[dep].setVisibility());
    // }
    this.setVisibility();
    return el;
  }

  resize = () => this.hand.resize();

  toggleEnabled = (dlg: Dialog, action: Action, e: Event) => {
    const cbox = e.target as HTMLInputElement;
    //const setting = this.els[cbox.parentElement!.id];
    this.settings.byEl(cbox)?.setEnabled(cbox.checked);
    this.bot.update();
  };

  valueChange = (dlg: Dialog, action: Action, e: Event) => {
    const setting = this.settings.byEvent(e);
    setting!.update();
    setting!.setEnabled();
    this.bot.update();
  };

  setVisibility = () => this.settings.forEach(el => el.setVisibility());

  showPanel = (dlg: Dialog, action: Action, e: Event) => {
    /*let div = e.target as HTMLElement;
    while (!div.id) div = div.parentElement!;
    $('.has-panel', this.view).each((_, el) =>
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
    const mapping = this.getProperty(div.id) as Mapping;
    if (!('data' in mapping && 'scale' in mapping)) throw new Error(`error on ${div.id}`);
    editPanel.dataset.panelShowing = div.id;
    const canvas = $as<HTMLCanvasElement>(`<canvas id="poop"></canvas>`);
    editPanel.innerHTML = '';
    editPanel.appendChild(canvas);
    renderMapping(canvas, mapping);*/
  };

  get bot(): ZerofishBotEditor {
    return this.bots[this.uid];
  }

  get botDefault(): BotInfoReader {
    return this.botCtrl.botDefaults[this.uid];
  }
  get selected(): string[] {
    return this.view.querySelector('.has-panel.selected')?.id.split('_') ?? [];
  }

  updateView = (force: boolean = false) => {
    if (force) {
      this.view.querySelector('.edit-bot')?.replaceWith(this.makeEditView());
      this.dlg?.refresh();
    }
    //renderMapping(this.view.querySelector('.edit-panel canvas') as HTMLCanvasElement, this.bot.mix!);
    if (force) this.hand.resize();
  };

  selectBot(uid = this.botCtrl[this.color]!.uid) {
    this.uid = uid;
    this.view.style.setProperty(`---${this.color}-image-url`, `url(${this.bot.imageUrl})`);
    this.view.querySelector('.edit-bot')?.replaceWith(this.makeEditView());
    //this.placardEl.textContent = this.bot.description;
    this.dlg?.refresh();
    this.setBot(this.uid);
  }
  /*
  setProperty = (id: string, value?: string | number) => {
    const path = objectPath(id);
    if (value === undefined) removePath({ obj: this.bot, path });
    else setPath({ obj: this.bot, path, value: value });
  };

  getProperty = (id: string) => {
    return objectPath(id)?.reduce((obj, key) => {
      return obj?.[key];
    }, this.bot);
  };
*/
  get playerEl() {
    return this.view.querySelector('.player') as HTMLElement;
  }

  get placardEl() {
    return this.view.querySelector('.placard') as HTMLElement;
  }

  clearBots = async (uids?: string[]) => {
    await this.botCtrl.clearLocalBots(uids);
    this.bots = this.botCtrl.zerofishBots;
    this.selectBot(this.bot.uid in this.bots ? this.bot.uid : Object.keys(this.bots)[0]);
    alert(uids ? `Cleared ${uids.join(' ')}` : 'Local bots cleared'); // make a flash for this stuff
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

// function objectPath(id: string) {
//   return id.split('_').slice(1);
// }

// function removePath({ obj, path }: { obj: any; path: string[] }) {
//   if (!obj) return;
//   if (path.length > 1) removePath({ obj: obj[path[0]], path: path.slice(1) });
//   if (typeof obj[path[0]] !== 'object' || Object.keys(obj[path[0]].length === 0)) delete obj[path[0]];
// }

// function setPath({ obj, path, value }: { obj: any; path: string[]; value: any }) {
//   if (path.length === 0) return;
//   if (path.length === 1) obj[path[0]] = value;
//   else if (!(path[0] in obj)) obj[path[0]] = {};
//   setPath({ obj: obj[path[0]], path: path.slice(1), value });
// }

// TODO - fetch these from the server
const lifatNets: { [name: string]: number } = {
  'badgyal-8.pb': 3,
  'evilgyal-6.pb': 3,
  'goodgyal-5.pb': 3,
  'tinygyal-8.pb': 4,
  'naise700.pb': 0,
  'nocap2000.pb': 0,
  'maia-1100.pb': 0,
  'maia-1200.pb': 0,
  'maia-1300.pb': 0,
  'maia-1400.pb': 0,
  'maia-1500.pb': 0,
  'maia-1600.pb': 0,
  'maia-1700.pb': 0,
  'maia-1800.pb': 0,
  'maia-1900.pb': 0,
};

const lifatBooks = [
  { name: 'Book.bin', value: { lichess: 'Book.bin', url: undefined } },
  { name: 'codekiddy.bin', value: { lichess: 'codekiddy.bin', url: undefined } },
  { name: 'DCbook_large.bin', value: { lichess: 'DCbook_large.bin', url: undefined } },
  { name: 'Elo2400.bin', value: { lichess: 'Elo2400.bin', url: undefined } },
  { name: 'final-book.bin', value: { lichess: 'final-book.bin', url: undefined } },
  { name: 'gavibook.bin', value: { lichess: 'gavibook.bin', url: undefined } },
  { name: 'gavibook-small.bin', value: { lichess: 'gavibook-small.bin', url: undefined } },
  { name: 'gm2600.bin', value: { lichess: 'gm2600.bin', url: undefined } },
  { name: 'komodo.bin', value: { lichess: 'komodo.bin', url: undefined } },
  { name: 'KomodoVariety.bin', value: { lichess: 'KomodoVariety.bin', url: undefined } },
  { name: 'Performance.bin', value: { lichess: 'Performance.bin', url: undefined } },
  { name: 'varied.bin', value: { lichess: 'varied.bin', url: undefined } },
];
