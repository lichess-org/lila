import type { Libot, Mapping } from '../types';
import type { BotInfoReader, SettingHost } from './types';
import type { ZerofishBotEditor, ZerofishBots } from '../zerofishBot';
import { BotCtrl } from '../botCtrl';
import { HandOfCards } from '../handOfCards';
import { defined, escapeHtml, enumerableEquivalence } from 'common';
import { GameCtrl } from '../gameCtrl';
import { buildFromSchema, SettingGroup } from './settingGroup';
import { objectPath, removePath } from './settingNode';
import * as licon from 'common/licon';

export class BotDialog implements SettingHost {
  view: HTMLElement;
  hand: HandOfCards;
  uid: string;
  scratch: { [uid: string]: ZerofishBotEditor } = {};
  settings: SettingGroup;
  dlg: Dialog;

  constructor(
    readonly botCtrl: BotCtrl,
    readonly gameCtrl: GameCtrl,
    readonly color: Color = 'white',
    readonly onSelectBot?: (uid: string) => void,
  ) {
    this.view = $as<HTMLElement>(`<div class="with-hand-of-cards"><div class="edit-bot"></div></div>`);
    this.selectBot();
    this.hand = new HandOfCards({
      view: () => this.view,
      drops: () => [{ el: this.view.querySelector('.player')!, selected: this.bot.uid }],
      cardData: () =>
        Object.values(this.bots)
          .map(b => b.card)
          .filter(defined),
      select: (_: HTMLElement, cardId?: string) => this.selectBot(`#${cardId}`),
      autoResize: false,
    });
  }

  get actions(): Action[] {
    return [
      ...this.settings.actions,
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
      site.dialog
        .dom({
          css: [{ hashed: 'local.test.setup' }],
          htmlText: `<div class="chin"><span>
            <button class="button button-green bot-apply">apply</button>
            <button class="button button-metal bot-json-one">json</button>
            <button class="button button-metal button-red bot-unrate-one">clear rating</button>
            <button class="button button-empty button-red bot-clear-one">revert</button>
            </span><span>
            <button class="button button-metal bot-json-all">all json</button>
            <button class="button button-empty button-red bot-unrate-all">clear all ratings</button>
            <button class="button button-empty button-red bot-clear-all">revert all to server</button>
          </span></div>`,
          append: [{ node: this.view, where: '.chin', how: 'before' }],
          actions: this.actions,
          onClose: () => {
            window.removeEventListener('resize', this.resize);
            resolve();
          },
        })
        .then(dlg => {
          this.dlg = dlg;
          this.update();
          this.dlg.show();
          window.addEventListener('resize', this.resize);
          this.resize();
        });
    });
  }

  makeEditView() {
    this.settings = new SettingGroup();
    const el = $as<HTMLElement>(`<div class="edit-bot">`);
    this.view.querySelector('.edit-bot')?.replaceWith(el);
    const playerInfo = $as<HTMLElement>(`<div class="player-info">`);
    playerInfo.appendChild(buildFromSchema(this, ['bot_name']).div);
    const player = $as<HTMLElement>(`<div class="player ${this.color}">`);
    player.appendChild(buildFromSchema(this, ['bot_description']).div);
    playerInfo.appendChild(player);
    el.appendChild(playerInfo);
    el.appendChild(buildFromSchema(this, ['bot']).div);
    const edit = $as<HTMLElement>(`<div class="edit-panel none">
        <div class="chart-wrapper"><canvas></canvas></div>
      </div>`);
    el.appendChild(edit);
    this.settings.forEach(el => el.setEnabled());
  }

  resize = () => this.hand.resize();

  get bots(): ZerofishBots {
    return this.botCtrl.zerofishBots;
  }

  get bot(): ZerofishBotEditor {
    if (!this.scratch[this.uid]) {
      const clone = structuredClone(this.bots[this.uid]);
      const editor = Object.defineProperty(clone, 'disabled', { value: new Set<string>() });
      this.scratch[this.uid] = editor as ZerofishBotEditor;
    }
    return this.scratch[this.uid];
  }

  get botDefault(): BotInfoReader {
    return this.botCtrl.botDefault(this.uid);
  }

  apply() {
    for (const id of this.bot.disabled) {
      if (this.bot.disabled.has(id)) removePath({ obj: this.bot, path: objectPath(id) }, true);
    }
    this.bot.disabled.clear();
    this.botCtrl.setBot(this.bot);
    this.selectBot();
  }

  get isClean() {
    return (
      !this.scratch[this.uid] || enumerableEquivalence(this.botCtrl.bot(this.uid), this.scratch[this.uid])
    );
  }

  get allDirty(): string[] {
    return Object.keys(this.scratch).filter(
      uid => !enumerableEquivalence(this.botCtrl.bot(uid), this.scratch[uid]),
    );
  }

  update() {
    const isClean = this.isClean;
    //const isAllClean = isClean && this.allDirty.length === 0;
    this.dlg?.view.querySelector('.bot-apply')?.classList.toggle('disabled', isClean);
    //this.dlg?.view.querySelector('.bot-clear-one')?.classList.toggle('disabled', !isClean);
    //this.dlg?.view.querySelector('.bot-clear-all')?.classList.toggle('disabled', !isAllClean);
  }

  selectBot(uid = this.uid ?? this.botCtrl[this.color]!.uid) {
    this.uid = uid;
    this.makeEditView();
    this.dlg?.actions(this.actions);
    this.update();
    this.onSelectBot?.(this.uid);
  }

  clearBots = async (uids?: string[]) => {
    for (const uid of uids ?? Object.keys(this.bots)) {
      delete this.scratch[uid];
    }
    await this.botCtrl.clearLocalBots(uids);
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
