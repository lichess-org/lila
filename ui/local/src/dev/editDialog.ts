import { type BotCtrl, domIdToUid, uidToDomId } from '../botCtrl';
import { handOfCards, type HandOfCards, type CardData } from '../handOfCards';
import { defined, escapeHtml, frag, deepFreeze } from 'common';
import { buildFromSchema, Panes } from './panes';
import { removeObjectProperty, closeEnough, deadStrip } from './devUtil';
import * as licon from 'common/licon';
import { domDialog, alert, confirm, type Dialog, type Action } from 'common/dialog';
import type { BotInfo } from '../types';
import { Bot, type Bots } from '../bot';
import type { ReadableBot, WritableBot } from './devTypes';
import type { GameCtrl } from '../gameCtrl';
import type { DevAssets } from './devAssets';
import { assetDialog } from './assetDialog';
import { shareDialog } from './shareDialog';

export class EditDialog {
  static default: BotInfo = deepFreeze<BotInfo>({
    uid: '#default',
    name: 'Name',
    description: 'Description',
    image: 'gray-torso.png',
    books: [],
    fish: { multipv: 1, by: { depth: 1 } },
    version: 0,
    ratings: {},
    operators: {},
    sounds: {},
  });

  view: HTMLElement;
  deck: HandOfCards;
  dlg: Dialog;
  uid: string;
  panes: Panes;
  scratch: { [uid: string]: WritableBot } = {}; // scratchpad for bot edits, pre-apply
  cleanups: (() => void)[] = []; // for chart.js

  constructor(
    readonly botCtrl: BotCtrl,
    readonly gameCtrl: GameCtrl,
    readonly color: Color = 'white',
    readonly onSelectBot?: (uid: string) => void,
  ) {
    this.view = frag<HTMLElement>(`<div class="base-view dev-view edit-view with-cards">
        <div class="edit-bot"></div>
      </div>`);
    this.selectBot();
    this.deck = handOfCards({
      view: this.view,
      getDrops: () => [{ el: this.view.querySelector('.player')!, selected: uidToDomId(this.bot.uid) }],
      deck: this.view.querySelector('.placeholder') as HTMLElement,
      getCardData: () => this.cardData,
      select: (_, domId?: string) => this.selectBot(domIdToUid(domId)),
    });
  }

  async show(): Promise<Dialog> {
    this.dlg = await domDialog({
      append: [{ node: this.view }],
      actions: this.actions,
      onClose: () => {
        window.removeEventListener('resize', this.deck.resize);
        for (const cleanup of this.cleanups) cleanup();
      },
    });
    window.addEventListener('resize', this.deck.resize);
    setTimeout(this.deck.resize);
    return this.dlg.show();
  }

  private lastCardData: CardData[];

  update(): void {
    this.dlg?.updateActions(this.actions);
    this.deck?.update();
    const isClean = !this.isDirty(this.botCtrl.get(this.uid)!);
    const canShare = isClean && this.localChanges;
    this.dlg?.view.querySelector('[data-bot-action="save-one"]')?.classList.toggle('none', isClean);
    this.dlg?.view
      .querySelector('[data-bot-action="pull-one"]')
      ?.classList.toggle('none', !this.localChanges);
    this.dlg?.view.querySelector('[data-bot-action="share-one"]')?.classList.toggle('none', !canShare);
  }

  get bot(): WritableBot {
    this.scratch[this.uid] ??= Object.defineProperty(structuredClone(this.bots[this.uid]), 'disabled', {
      value: new Set<string>(),
    }) as WritableBot; // scratchpad for bot edits
    return this.scratch[this.uid];
  }

  get localBot(): ReadableBot | undefined {
    return this.botCtrl.localBots[this.uid];
  }

  get serverBot(): ReadableBot | undefined {
    return this.botCtrl.serverBots[this.uid];
  }

  get assets(): DevAssets {
    return this.botCtrl.assets as DevAssets;
  }

  private get bots(): Bots {
    return this.botCtrl.bots;
  }

  private get actions(): Action[] {
    return [
      ...this.panes.actions,
      { selector: '[data-bot-action="save-one"]', listener: () => this.save() },
      { selector: '[data-bot-action="new"]', listener: () => this.newBot() },
      { selector: '[data-bot-action="delete"]', listener: () => this.deleteBot() },
      { selector: '[data-bot-action="json-one"]', listener: () => this.showJson([this.bot.uid]) },
      { selector: '[data-bot-action="json-all"]', listener: () => this.showJson() },
      //{ selector: '[data-bot-action="unrate-one"]', listener: () => this.clearRatings([this.bot.uid]) },
      { selector: '[data-bot-action="assets"]', listener: () => assetDialog(this.assets) },
      { selector: '[data-bot-action="share-one"]', listener: () => shareDialog(this, this.bot.uid) },
      { selector: '[data-bot-action="pull-one"]', listener: () => this.clearBots([this.bot.uid]) },
      { selector: '[data-bot-action="pull-all"]', listener: () => this.clearBots() },
      { selector: '.player', listener: e => this.clickImage(e) },
    ];
  }

  private isDirty = (other: BotInfo | undefined): boolean => {
    if (!other || !this.scratch[other.uid]) return false;
    return !closeEnough(other, deadStrip(this.scratch[other.uid]));
  };

  private get localChanges(): boolean {
    return this.localBot !== undefined && !closeEnough(this.localBot, this.serverBot);
  }

  private get cardData() {
    const speed = 'classical'; //this.gameCtrl.speed;
    return Object.values({ ...this.bots, ...this.scratch })
      .sort((a, b) => a.name.localeCompare(b.name))
      .sort((a, b) => (a.ratings[speed]?.r ?? 1500) - (b.ratings[speed]?.r ?? 1500))
      .map(bot => this.botCtrl.classifiedCard(bot, this.isDirty))
      .filter(defined)
      .sort(this.botCtrl.classifiedSort);
  }

  private save() {
    const [sourcesScroll, operatorsScroll] = [
      this.view.querySelector('.sources')!.scrollTop,
      this.view.querySelector('.operators')!.scrollTop,
    ];
    for (const id of this.bot.disabled) removeObjectProperty({ obj: this.bot, path: { id } }, true);
    this.bot.disabled.clear();
    this.botCtrl.saveBot(this.bot);
    delete this.scratch[this.uid];
    this.selectBot();
    this.view.querySelector('.sources')!.scrollTop = sourcesScroll ?? 0;
    this.view.querySelector('.operators')!.scrollTop = operatorsScroll ?? 0;
  }

  private selectBot(uid = this.uid ?? this.botCtrl[this.color]?.uid) {
    this.uid = uid;
    this.makeEditView();
    this.update();
    this.onSelectBot?.(this.uid);
  }

  private async clickImage(e: Event) {
    if (e.target !== e.currentTarget) return;
    const newImage = await assetDialog(this.assets, 'image');
    if (!newImage) return;
    this.bot.image = newImage;
    this.update();
  }

  private makeEditView(): void {
    for (const cleanup of this.cleanups) cleanup();
    this.cleanups = [];
    this.panes = new Panes();
    const el = this.view.querySelector('.edit-bot') as HTMLElement;
    el.innerHTML = '';
    el.appendChild(this.botCardEl);
    const sources = buildFromSchema(this, ['sources']).el;
    sources.prepend(this.botInfoEl);
    el.appendChild(sources);
    el.appendChild(buildFromSchema(this, ['bot_operators']).el);
    el.appendChild(this.deckEl);
    el.appendChild(this.globalActionsEl);
    this.panes.forEach(el => el.setEnabled());
  }

  private clearBots = async (uids?: string[]) => {
    if (!(await confirm(uids ? `Pull ${uids.join(' ')}?` : 'Pull all server bots?'))) return;
    for (const uid of uids ?? Object.keys(this.bots)) {
      delete this.scratch[uid];
    }
    await this.botCtrl.clearStoredBots(uids);
    this.selectBot(this.bot.uid in this.bots ? this.bot.uid : Object.keys(this.bots)[0]);
    //alert(uids ? `Cleared ${uids.join(' ')}` : 'Local bots cleared'); // need a flash for this stuff
  };

  // private clearRatings = (uids: string[] = Object.keys(this.bots)) => {
  //   for (const uid of uids) {
  //     if (this.scratch[uid]) this.scratch[uid].glicko = undefined;
  //     if (!Object.keys(this.bots[uid].ratings)) continue;
  //     this.bots[uid].ratings = {};
  //     this.botCtrl.storeRating(uid);
  //   }
  //   this.selectBot();
  //   this.gameCtrl.redraw();
  // };

  private async showJson(uids?: string[]): Promise<void> {
    const text = escapeHtml(
      JSON.stringify(uids ? uids.map(id => this.bots[id]) : [...Object.values(this.bots)], null, 2),
    );
    const clear = text ? `<button class="button button-empty button-red clear">clear local</button>` : '';
    const copy = `<button class="button copy" data-icon="${licon.Clipboard}"> copy</button>`;
    const dlg = await domDialog({
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
  }

  private deleteBot(): void {
    confirm(`Delete ${this.uid}?`).then(async ok => {
      if (!ok) return;
      delete this.scratch[this.uid];
      this.botCtrl.deleteBot(this.uid).then(() => this.selectBot());
      this.update();
    });
  }

  private newBot(): void {
    const ok = frag<HTMLButtonElement>('<button class="button ok disabled">ok</button>');
    const input = frag<HTMLInputElement>(`<input class="invalid" spellcheck="false" type="text" value="#">`);
    domDialog({
      class: 'dev-view',
      htmlText: `<h2>Choose a user id</h2><p>must be unique and begin with #</p><span></span>`,
      append: [
        { node: input, where: 'span' },
        { node: ok, where: 'span' },
      ],
      focus: 'input',
      actions: [
        {
          selector: 'input',
          event: ['input'],
          listener: () => {
            const newUid = input.value.toLowerCase();
            const isValid = /^#[a-z][a-z0-9-]{2,19}$/.test(newUid) && this.bots[newUid] === undefined;
            input.dataset.uid = isValid ? newUid : '';
            input.classList.toggle('invalid', !isValid);
            ok.classList.toggle('disabled', !isValid);
          },
        },
        {
          selector: 'input',
          event: ['keydown'],
          listener: e => {
            if ('key' in e && e.key === 'Enter') ok.click();
          },
        },
        {
          selector: '.ok',
          listener: (_, dlg) => {
            if (!input.dataset.uid) return;
            const newBot = {
              ...structuredClone(EditDialog.default),
              uid: input.dataset.uid,
              name: input.dataset.uid.slice(1),
            } as WritableBot;
            this.botCtrl.saveBot(newBot);
            this.selectBot(newBot.uid);
            dlg.close();
          },
        },
      ],
    }).then(dlg => {
      input.setSelectionRange(1, 1);
      dlg.showModal();
    });
  }

  private deckEl = frag<HTMLElement>(`<div class="deck">
        <div class="placeholder"></div>
        <fieldset class="deck-legend">
          <legend>legend</legend>
          <label class="clean dirty">dirty</label>
          <label class="local-only">unshared</label>
          <label class="local-changes">local changes</label>
          <label class="upstream-changes">upstream changes</label>
        </fieldset>
      </div>`);

  private globalActionsEl = frag<HTMLElement>(`<div class="global-actions">
        <button class="button button-empty button-green" data-bot-action="new">new bot</button>
        <button class="button button-empty button-brag" data-bot-action="assets">assets</button>
        <button class="button button-empty" data-bot-action="pull-all">pull all server</button>
      </div>`);

  private botActionsEl = frag<HTMLElement>(`<div class="bot-actions">
          <button class="button button-empty button-red" data-bot-action="delete">delete</button>
          <button class="button button-empty button-dim" data-bot-action="json-one">json</button>
          <button class="button button-empty none" data-bot-action="pull-one">server</button>
          <button class="button button-empty button-brag none" data-bot-action="share-one">share</button>
          <button class="button button-green none" data-bot-action="save-one">save</button>
        </div>`);

  private get botCardEl(): Node {
    const botCard = frag<Element>(`<div class="bot-card">
        <div class="player ${this.color}"><span>${this.uid}</span></div>
      </div>`);
    botCard.append(this.botActionsEl);
    botCard.firstElementChild?.appendChild(buildFromSchema(this, ['bot_description']).el);
    return botCard;
  }

  private get botInfoEl(): Node {
    //const bot = this.botCtrl.get(this.uid) as WritableBot;
    //const glicko = bot.glicko ?? { r: 1500, rd: 350 };
    const info = frag<Element>(`<span class="bot-info">`);
    /*if (glicko.rd !== 350)
      info.append(
        frag(
          `<span><label>rating</label><button data-bot-action="unrate-one">${bot.fullRatingText}</button></span>`,
        ),
      );
    else */ info.append(frag(`<label>rating ${this.botCtrl.fullRatingText(this.uid, 'classical')}?</label>`));
    info.prepend(buildFromSchema(this, ['bot_name']).el);
    return info;
  }
}
