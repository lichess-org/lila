import { type BotCtrl, domIdToUid, uidToDomId } from '../botCtrl';
import { HandOfCards } from '../handOfCards';
import { defined, escapeHtml, isEquivalent } from 'common';
import { buildFromSchema, PaneCtrl } from './paneCtrl';
import { removeObjectProperty } from './util';
import * as licon from 'common/licon';
import { domDialog, alert, confirm, type Dialog, type Action } from 'common/dialog';
import type { BotInfoReader, ZerofishBotEditor, HostView } from './types';
import type { ZerofishBots } from '../zerofishBot';
import type { GameCtrl } from '../gameCtrl';
import type { DevRepo } from './devRepo';
import { assetDialog } from './assetDialog';
import { shareDialog } from './shareDialog';

export class EditDialog implements HostView {
  view: HTMLElement;
  hand: HandOfCards;
  dlg: Dialog;
  uid: string;
  ctrl: PaneCtrl;
  scratch: { [uid: string]: ZerofishBotEditor } = {}; // scratchpad for bot edits, pre-apply
  cleanups: (() => void)[] = []; // chart.js

  constructor(
    readonly botCtrl: BotCtrl,
    readonly gameCtrl: GameCtrl,
    readonly color: Color = 'white',
    readonly onSelectBot?: (uid: string) => void,
  ) {
    this.view = $as<HTMLElement>(`<div class="base-view dev-view edit-view with-cards">
        <div class="edit-bot"></div>
        <div class="deck"></div>
      </div>`);
    this.selectBot();
    this.hand = new HandOfCards({
      view: () => this.view,
      drops: () => [{ el: this.view.querySelector('.player')!, selected: uidToDomId(this.bot.uid) }],
      cardData: () =>
        Object.values({ ...this.bots, ...this.scratch })
          .map(b => botCtrl.card(b))
          .filter(defined),
      select: (_: HTMLElement, domId?: string) => this.selectBot(domIdToUid(domId)),
      deck: () => this.view.querySelector('.deck')!, // TODO optimize
      autoResize: false,
    });
  }

  async show(): Promise<Dialog> {
    this.dlg = await domDialog({
      append: [{ node: this.view }],
      actions: this.actions,
      noClickAway: true,
      onClose: () => {
        window.removeEventListener('resize', this.hand.resize);
        for (const cleanup of this.cleanups) cleanup();
      },
    });
    window.addEventListener('resize', this.hand.resize);
    setTimeout(this.hand.resize);
    return this.dlg.show();
  }

  update(): void {
    this.dlg?.updateActions(this.actions);
    const isClean = this.isClean;
    this.dlg?.view.querySelector('.bot-apply')?.classList.toggle('disabled', isClean);
  }

  get bot(): ZerofishBotEditor {
    this.scratch[this.uid] ??= Object.defineProperty(structuredClone(this.bots[this.uid]), 'disabled', {
      value: new Set<string>(),
    }) as ZerofishBotEditor; // scratchpad for bot edits
    return this.scratch[this.uid];
  }

  get defaultBot(): BotInfoReader {
    return this.botCtrl.defaultBot(this.uid);
  }

  get assetDb(): DevRepo {
    return this.botCtrl.assetDb as DevRepo;
  }

  private makeEditView(): void {
    for (const cleanup of this.cleanups) cleanup();
    this.cleanups = [];
    this.ctrl = new PaneCtrl();
    const el = this.view.querySelector('.edit-bot') as HTMLElement;
    el.innerHTML = this.globalActionsHtml;
    el.appendChild(this.botCardEl);
    const sources = buildFromSchema(this, ['sources']).el;
    sources.prepend(this.botInfoEl);
    el.appendChild(sources);
    el.appendChild(buildFromSchema(this, ['bot_operators']).el);

    this.ctrl.forEach(el => el.setEnabled());
  }

  private apply() {
    const [sourcesScroll, operatorsScroll] = [
      this.view.querySelector('.sources')!.scrollTop,
      this.view.querySelector('.operators')!.scrollTop,
    ];
    console.log(this.bot);
    for (const id of this.bot.disabled) {
      removeObjectProperty({ obj: this.bot, path: { id } }, true);
      this.ctrl.dependsOn(id).forEach(r => removeObjectProperty({ obj: this.bot, path: { id: r.id } }, true));
    }
    this.bot.disabled.clear();
    this.botCtrl.updateBot(this.bot);
    delete this.scratch[this.uid];
    this.selectBot();
    this.view.querySelector('.sources')!.scrollTop = sourcesScroll ?? 0;
    this.view.querySelector('.operators')!.scrollTop = operatorsScroll ?? 0;
  }

  private selectBot(uid = this.uid ?? this.botCtrl[this.color]!.uid) {
    this.uid = uid;
    this.makeEditView();
    this.update();
    this.onSelectBot?.(this.uid);
  }

  private async clickImage(e: Event) {
    if (e.target !== e.currentTarget) return;
    const newImage = await assetDialog(this.assetDb, 'image');
    if (!newImage) return;
    this.bot.image = newImage;
    this.hand.updateCards();
    this.update();
  }

  private clearBots = async (uids?: string[]) => {
    if (!(await confirm(uids ? `Clear ${uids.join(' ')}?` : 'Clear all local bots?'))) return;
    for (const uid of uids ?? Object.keys(this.bots)) {
      delete this.scratch[uid];
    }
    await this.botCtrl.clearLocalBots(uids);
    this.selectBot(this.bot.uid in this.bots ? this.bot.uid : Object.keys(this.bots)[0]);
    alert(uids ? `Cleared ${uids.join(' ')}` : 'Local bots cleared'); // make a flash for this stuff
  };

  private clearRatings = (uids: string[] = Object.keys(this.bots)) => {
    for (const uid of uids) {
      if (this.scratch[uid]) this.scratch[uid].glicko = undefined;
      if (!this.bots[uid].glicko) continue;
      this.bots[uid].glicko = undefined;
      this.botCtrl.storeBot(uid);
    }
    this.selectBot();
    this.gameCtrl.redraw();
  };

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
    });
  }

  private newBot(): void {
    const ok = $as<HTMLButtonElement>('<button class="button ok disabled">ok</button>');
    const input = $as<HTMLInputElement>(`<input class="invalid" spellcheck="false" type="text" value="#">`);
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
              ...structuredClone(this.botCtrl.default),
              uid: input.dataset.uid,
              name: input.dataset.uid.slice(1),
            } as ZerofishBotEditor;
            this.botCtrl.updateBot(newBot);
            this.selectBot(newBot.uid);
            this.hand.updateCards();
            dlg.close();
          },
        },
      ],
    }).then(dlg => {
      input.setSelectionRange(1, 1);
      dlg.showModal();
    });
  }

  private get bots(): ZerofishBots {
    return this.botCtrl.zerofishBots;
  }

  private get actions(): Action[] {
    return [
      ...this.ctrl.actions,
      { selector: '.bot-apply', listener: () => this.apply() },
      { selector: '.bot-new', listener: () => this.newBot() },
      { selector: '.bot-delete', listener: () => this.deleteBot() },
      { selector: '.bot-json-one', listener: () => this.showJson([this.bot.uid]) },
      { selector: '.bot-json-all', listener: () => this.showJson() },
      { selector: '.bot-unrate-one', listener: () => this.clearRatings([this.bot.uid]) },
      { selector: '.bot-assets', listener: () => assetDialog(this.assetDb) },
      { selector: '.bot-share', listener: () => shareDialog(this.botCtrl, this.bot.uid) },
      { selector: '.bot-clear-one', listener: () => this.clearBots([this.bot.uid]) },
      { selector: '.bot-clear-all', listener: () => this.clearBots() },
      { selector: '.player', listener: e => this.clickImage(e) },
    ];
  }

  private get isClean(): boolean {
    if (!this.scratch[this.uid]) return true;
    const scratch = structuredClone(this.scratch[this.uid]);
    for (const id of this.bot.disabled) {
      removeObjectProperty({ obj: scratch, path: { id } }, true);
    }
    return isEquivalent(this.botCtrl.bot(this.uid), scratch);
  }

  private get allDirty(): string[] {
    return Object.keys(this.scratch).filter(uid => !isEquivalent(this.botCtrl.bot(uid), this.scratch[uid]));
  }

  private get globalActionsHtml(): string {
    return `<div class="global-actions">
        <button class="button button-empty button-green bot-new">new bot</button>
        <button class="button button-empty button-brag bot-share">share</button>
        <button class="button button-empty button-brag bot-assets">assets</button>
        <button class="button button-empty button-red bot-clear-all">revert all to server</button>
      </div>`;
  }

  private get botCardEl(): Node {
    const botCard = $as<Element>(`<div class="bot-card">
        <div class="player ${this.color}"><span>${this.uid}</span></div>
        <div class="bot-actions">
          <button class="button button-empty button-red bot-delete">delete</button>
          <button class="button button-empty button-dim bot-json-one">json</button>
          <button class="button button-empty button-red bot-clear-one">revert</button>
          <button class="button button-green bot-apply disabled">apply</button>
        </div>
      </div>`);
    botCard.firstElementChild?.appendChild(buildFromSchema(this, ['bot_description']).el);
    return botCard;
  }

  private get botInfoEl(): Node {
    const bot = this.botCtrl.bot(this.uid) as ZerofishBotEditor;
    const glicko = bot.glicko ?? { r: 1500, rd: 350 };
    const info = $as<Element>(`<span class="bot-info">
        <span><label>rating</label>${bot.fullRatingText}${
          glicko.rd !== 350 ? `<i class="bot-unrate-one" data-icon="${licon.Cancel}"></i>` : ''
        }</span>
      </span>`);
    info.prepend(buildFromSchema(this, ['bot_name']).el);
    return info;
  }
}
