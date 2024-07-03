import type { Libot, Mapping } from '../types';
import type { BotInfoReader, EditorHost } from './types';
import type { ZerofishBotEditor, ZerofishBots } from '../zerofishBot';
import { BotCtrl } from '../botCtrl';
import { HandOfCards } from '../handOfCards';
import { defined, escapeHtml, enumerableEquivalence } from 'common';
import { GameCtrl } from '../gameCtrl';
import { buildFromSchema, Editor } from './editor';
import { /*objectPath,*/ removeObjectProperty } from './util';
import * as licon from 'common/licon';
import { domDialog, alert, confirm, Dialog, Action } from 'common/dialog';

export class EditDialog implements EditorHost {
  view: HTMLElement;
  hand: HandOfCards;
  dlg: Dialog;
  uid: string;
  editor: Editor;
  scratch: { [uid: string]: ZerofishBotEditor } = {};
  cleanups: (() => void)[] = [];

  constructor(
    readonly botCtrl: BotCtrl,
    readonly gameCtrl: GameCtrl,
    readonly color: Color = 'white',
    readonly onSelectBot?: (uid: string) => void,
  ) {
    this.view = $as<HTMLElement>(`<div class="with-hand-of-cards">
        <div class="edit-bot"></div>
        <div class="deck"></div>
      </div>`);
    this.selectBot();
    this.hand = new HandOfCards({
      view: () => this.view,
      drops: () => [{ el: this.view.querySelector('.player')!, selected: this.bot.uid }],
      cardData: () =>
        Object.values(this.bots)
          .map(b => b.card)
          .filter(defined),
      select: (_: HTMLElement, cardId?: string) => this.selectBot(`#${cardId}`),
      deck: () => this.view.querySelector('.deck')!,
      autoResize: false,
    });
  }

  get actions(): Action[] {
    return [
      ...this.editor.actions,
      { selector: '.bot-apply', listener: () => this.apply() },
      { selector: '.bot-json-one', listener: () => this.showJson([this.bot.uid]) },
      { selector: '.bot-json-all', listener: () => this.showJson() },
      { selector: '.bot-unrate-one', listener: () => this.clearRatings([this.bot.uid]) },
      { selector: '.bot-unrate-all', listener: () => this.clearRatings() },
      { selector: '.bot-clear-one', listener: () => this.clearBots([this.bot.uid]) },
      { selector: '.bot-clear-all', listener: () => this.clearBots() },
    ];
  }

  show() {
    return new Promise<void>(resolve => {
      domDialog({
        append: [{ node: this.view }],
        actions: this.actions,
        onClose: () => {
          window.removeEventListener('resize', this.resize);
          this.destroy();
          resolve();
        },
      }).then(dlg => {
        this.dlg = dlg;
        this.update();
        this.dlg.show();
        window.addEventListener('resize', this.resize);
        this.resize();
      });
    });
  }

  destroy() {
    for (const cleanup of this.cleanups) cleanup();
  }

  makeEditView() {
    for (const cleanup of this.cleanups) cleanup();
    this.cleanups = [];
    this.editor = new Editor();
    const el = this.view.querySelector('.edit-bot') as HTMLElement;
    el.innerHTML = '';
    el.appendChild(this.botCardEl);
    const sources = buildFromSchema(this, ['sources']).el;
    sources.prepend(this.botInfoEl);
    el.appendChild(sources);
    el.appendChild(buildFromSchema(this, ['bot_selectors']).el);
    this.editor.forEach(el => el.setEnabled());
  }

  resize = () => this.hand.resize();

  get bots(): ZerofishBots {
    return this.botCtrl.zerofishBots;
  }

  get bot(): ZerofishBotEditor {
    if (!this.scratch[this.uid]) {
      const clone = structuredClone(this.bots[this.uid]);
      const scratch = Object.defineProperty(clone, 'disabled', { value: new Set<string>() });
      this.scratch[this.uid] = scratch as ZerofishBotEditor;
    }
    return this.scratch[this.uid];
  }

  get botDefault(): BotInfoReader {
    return this.botCtrl.botDefault(this.uid);
  }

  apply() {
    for (const id of this.bot.disabled) {
      removeObjectProperty({ obj: this.bot, path: { id } }, true);
      this.editor
        .requires(id)
        .forEach(r => removeObjectProperty({ obj: this.bot, path: { id: r.id } }, true));
    }
    this.bot.disabled.clear();
    this.botCtrl.setBot(this.bot);
    delete this.scratch[this.uid];
    this.selectBot();
  }

  get isClean() {
    if (!this.scratch[this.uid]) return true;
    const scratch = structuredClone(this.scratch[this.uid]);
    for (const id of this.bot.disabled) {
      removeObjectProperty({ obj: scratch, path: { id } }, true);
    }
    return enumerableEquivalence(this.botCtrl.bot(this.uid), scratch);
  }

  get allDirty(): string[] {
    return Object.keys(this.scratch).filter(
      uid => !enumerableEquivalence(this.botCtrl.bot(uid), this.scratch[uid]),
    );
  }

  update() {
    this.dlg?.updateActions(this.actions);
    const isClean = this.isClean;
    //const isAllClean = isClean && this.allDirty.length === 0;
    this.dlg?.view.querySelector('.bot-apply')?.classList.toggle('disabled', isClean);
    //this.dlg?.view.querySelector('.bot-clear-one')?.classList.toggle('disabled', !isClean);
    //this.dlg?.view.querySelector('.bot-clear-all')?.classList.toggle('disabled', !isAllClean);
    //this.dlg?.actions(this.actions);
  }

  selectBot(uid = this.uid ?? this.botCtrl[this.color]!.uid) {
    this.uid = uid;
    this.makeEditView();
    this.update();
    this.onSelectBot?.(this.uid);
  }

  clearBots = async (uids?: string[]) => {
    if (!(await confirm(uids ? `Clear ${uids.join(' ')}?` : 'Clear all local bots?'))) return;
    for (const uid of uids ?? Object.keys(this.bots)) {
      delete this.scratch[uid];
    }
    await this.botCtrl.clearLocalBots(uids);
    this.selectBot(this.bot.uid in this.bots ? this.bot.uid : Object.keys(this.bots)[0]);
    alert(uids ? `Cleared ${uids.join(' ')}` : 'Local bots cleared'); // make a flash for this stuff
  };

  clearRatings = (uids?: string[]) => {
    const unrate: Libot[] = [];
    if (uids) uids.forEach(uid => unrate.push(this.bots[uid], this.scratch[uid]));
    else Object.values(this.bots).forEach(bot => unrate.push(bot));
    unrate.filter(defined).forEach(bot => (bot.glicko = undefined));
    this.selectBot();
    this.gameCtrl.redraw();
  };

  showJson = async (uids?: string[]) => {
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
  };

  get globalActionsEl(): Node {
    return $as<Node>(`<div class="global-actions">
        <button class="button button-empty button-dim bot-json-all">all json</button>
        <button class="button button-empty button-red bot-unrate-all">clear all ratings</button>
        <button class="button button-empty button-red bot-clear-all">revert all to server</button>
      </div>`);
  }

  get botCardEl(): Node {
    const botCard = $as<Element>(`<div class="bot-card">
        <div class="player ${this.color}"><span>${this.uid}</span></div>
        <div class="bot-actions">
          <button class="button button-empty button-red bot-clear-one">revert</button>
          <button class="button button-empty button-dim bot-json-one">json</button>
          <button class="button button-green bot-apply">apply</button>
        </div>
      </div>`);
    botCard.firstElementChild?.appendChild(buildFromSchema(this, ['bot_description']).el);
    return botCard;
  }

  get botInfoEl(): Node {
    const bot = this.botCtrl.bot(this.uid) as ZerofishBotEditor;
    const glicko = bot.glicko ?? { r: 1500, rd: 350 };
    const info = $as<Element>(`<div class="bot-info">
        <span><label>rating</label>${bot.fullRatingText}${
          glicko.rd !== 350 ? `<i class="bot-unrate-one" data-icon="${licon.Cancel}"></i>` : ''
        }</span>
      </div>`);
    info.prepend(buildFromSchema(this, ['bot_name']).el);
    return info;
  }
}
