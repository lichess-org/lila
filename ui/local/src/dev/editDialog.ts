import { domIdToUid, uidToDomId } from '../botCtrl';
import { handOfCards, type HandOfCards } from '../handOfCards';
import { defined, frag } from 'common';
import { deepFreeze } from 'common/algo';
import { buildFromSchema, Panes } from './panes';
import { removeObjectProperty, closeEnough, deadStrip } from './devUtil';
import { domDialog, confirm, alert, type Dialog, type Action } from 'common/dialog';
import type { BotInfo } from '../types';
import { Bot } from '../bot';
import { AssetDialog, type AssetType } from './assetDialog';
import { historyDialog } from './historyDialog';
import { env } from '../localEnv';
import { pubsub } from 'common/pubsub';
import { Janitor } from 'common/event';

export class EditDialog {
  static default: BotInfo = deepFreeze<BotInfo>({
    uid: '#default',
    name: 'Name',
    description: 'Description',
    image: 'gray-torso.webp',
    books: [],
    fish: { multipv: 1, by: { depth: 1 } },
    version: 0,
    ratings: { classical: 1500 },
    filters: {},
    sounds: {},
  });

  view: HTMLElement;
  deck: HandOfCards;
  dlg: Dialog;
  assetDlg: AssetDialog | undefined;
  uid: string;
  panes: Panes;
  scratch: Record<string, WritableBot> = {}; // scratchpad for bot edits
  chartJanitor: Janitor = new Janitor();

  constructor(readonly color: Color) {
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
      opaque: true,
      center: 1 / 3,
    });
  }

  async show(): Promise<Dialog> {
    this.dlg = await domDialog({
      append: [{ node: this.view }],
      actions: this.actions,
      noClickAway: true,
      onClose: () => {
        pubsub.off('local.dev.import.book', this.onBookImported);
        window.removeEventListener('resize', this.deck.resize);
        this.chartJanitor.cleanup();
      },
    });
    pubsub.on('local.dev.import.book', this.onBookImported);
    window.addEventListener('resize', this.deck.resize);
    setTimeout(this.deck.resize);
    return this.dlg.show();
  }

  update(): void {
    this.dlg?.updateActions(this.actions);
    this.deck?.update();
    this.view.querySelector('[data-bot-action="save-one"]')?.classList.toggle('none', !this.isDirty());
    this.view
      .querySelector('[data-bot-action="pull-one"]')
      ?.classList.toggle('none', !this.isDirty() && !this.localChanges);
    this.view
      .querySelector('[data-bot-action="push-one"]')
      ?.classList.toggle('none', !env.canPost || this.isDirty() || !this.localChanges);
    this.view
      .querySelector('[data-bot-action="delete"]')
      ?.classList.toggle('none', !env.canPost && !this.serverBot);
  }

  assetDialog = async (type?: AssetType): Promise<string | undefined> => {
    this.assetDlg = new AssetDialog(type);
    const asset = await this.assetDlg.show();
    this.assetDlg = undefined;
    return asset;
  };

  get bot(): WritableBot {
    // side effects on a getter are horrible, but this one is lovely and wonderful
    this.scratch[this.uid] ??= Object.defineProperty(structuredClone(this.bots[this.uid]), 'disabled', {
      value: new Set<string>(),
    }) as WritableBot;
    return this.scratch[this.uid];
  }

  get localBot(): ReadableBot | undefined {
    return env.bot.localBots[this.uid];
  }

  get serverBot(): ReadableBot | undefined {
    return env.bot.serverBots[this.uid];
  }

  private get bots(): Record<string, Bot> {
    return env.bot.bots;
  }

  private get actions(): Action[] {
    return [
      ...this.panes.actions,
      { selector: '[data-bot-action="save-one"]', listener: () => this.save() },
      { selector: '[data-bot-action="new"]', listener: () => this.newBot() },
      { selector: '[data-bot-action="delete"]', listener: () => this.deleteBot() },
      { selector: '[data-bot-action="history-one"]', listener: () => historyDialog(this, this.uid) },
      //{ selector: '[data-bot-action="history-all"]', listener: () => historyDialog(this) },
      { selector: '[data-bot-action="unrate-all"]', listener: () => this.clearRatings() },
      { selector: '[data-bot-action="assets"]', listener: () => this.assetDialog() },
      { selector: '[data-bot-action="push-one"]', listener: () => this.push() },
      { selector: '[data-bot-action="pull-one"]', listener: () => this.pullBots([this.bot.uid]) },
      { selector: '[data-bot-action="pull-all"]', listener: () => this.pullBots() },
      { selector: '.player', listener: e => this.clickImage(e) },
    ];
  }

  private isDirty = (other: BotInfo | undefined = env.bot.get(this.uid)): boolean => {
    return (
      other !== undefined &&
      this.scratch[other.uid] !== undefined &&
      !closeEnough(other, deadStrip(this.scratch[other.uid]))
    );
  };

  private get localChanges(): boolean {
    return this.localBot !== undefined && !closeEnough(this.localBot, this.serverBot);
  }

  private get cardData() {
    const speed = 'classical'; //env.game.speed;
    return Object.values({ ...this.bots, ...this.scratch })
      .map(bot => env.bot.classifiedCard(bot, this.isDirty))
      .filter(defined)
      .sort(env.bot.classifiedSort(speed));
  }

  private async push() {
    const err = await env.push.pushBot(this.bot);
    if (err) return alert(err);
    delete this.scratch[this.uid];
    this.update();
  }

  private save() {
    const sourcesScroll = this.view.querySelector('.sources')!.scrollTop;
    const filtersScroll = this.view.querySelector('.filters')!.scrollTop;
    for (const id of this.bot.disabled) removeObjectProperty({ obj: this.bot, path: { id } }, true);
    this.bot.disabled.clear();
    env.bot.save(this.bot);
    delete this.scratch[this.uid];
    this.selectBot();
    this.view.querySelector('.sources')!.scrollTop = sourcesScroll ?? 0;
    this.view.querySelector('.filters')!.scrollTop = filtersScroll ?? 0;
  }

  private selectBot(uid = this.uid ?? env.bot[this.color]?.uid ?? env.bot.firstUid): void {
    if (!uid) return this.dlg.close();
    this.uid = uid;
    this.makeEditView();
    this.update();
  }

  private async deleteBot(): Promise<void> {
    if (!(await confirm(`Delete ${this.uid}?`))) return;

    const rsp = await fetch('/local/dev/bot', {
      method: 'post',
      headers: { 'Content-Type': 'application/json' },
      body: `{"uid":"${this.uid}"}`,
    });

    if (!rsp.ok) return;
    delete this.scratch[this.uid];
    await env.bot.delete(this.uid).then(() => this.selectBot(env.bot.firstUid));
    this.update();
  }

  private pullBots = async (uids?: string[]) => {
    if (!(await confirm(uids ? `Pull ${uids.join(' ')}?` : 'Pull all server bots?'))) return;
    const clear = (uids ?? Object.keys(this.bots)).filter(uid => env.bot.serverBots[uid]);
    clear.forEach(uid => delete this.scratch[uid]);
    await env.bot.clearStoredBots(clear);
    this.selectBot(this.bot.uid in this.bots ? this.bot.uid : Object.keys(this.bots)[0]);
  };

  private async clickImage(e: Event) {
    if (e.target !== e.currentTarget) return;
    const newImage = await this.assetDialog('image');
    if (!newImage) return;
    this.bot.image = newImage;
    this.update();
  }

  private makeEditView(): void {
    this.chartJanitor?.cleanup();
    this.panes = new Panes();
    const el = this.view.querySelector('.edit-bot') as HTMLElement;
    el.innerHTML = '';
    el.appendChild(this.botCardEl);
    el.appendChild(buildFromSchema(this, ['sources']).el);
    el.appendChild(buildFromSchema(this, ['bot_filters']).el);
    el.appendChild(this.deckEl);
    el.appendChild(this.globalActionsEl);
    this.panes.forEach(el => el.setEnabled());
  }

  private onBookImported = (key: string, oldKey?: string) => {
    this.assetDlg?.update();
    if (!oldKey) return;
    for (const bot of new Set<WritableBot>([this.bot, ...Object.values(this.scratch)])) {
      const existing = bot.books?.find(b => b.key === oldKey);
      if (existing) {
        existing.key = key;
        if (bot.uid === this.uid) this.selectBot();
      }
    }
    this.update();
  };

  private async clearRatings(): Promise<void> {
    await env.dev.clearRatings();
    alert('ratings cleared');
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
            if ('key' in e && e.key === 'Enter') {
              ok.click();
              e.stopPropagation();
              e.preventDefault();
            }
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
            env.bot.save(newBot);
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
      <button class="button button-empty" data-bot-action="pull-all">pull all</button>
      <button class="button button-empty button-dim" data-bot-action="unrate-all">clear all ratings</button>
    </div>`);

  private botActionsEl = frag<HTMLElement>(`<div class="bot-actions">
      <button class="button button-empty button-red none" data-bot-action="delete">delete</button>
      <button class="button button-empty button-brag" data-bot-action="history-one">history</button>
      <button class="button button-empty none" data-bot-action="pull-one">pull</button>
      <button class="button button-empty button-clas none" data-bot-action="push-one">push</button>
      <button class="button none" data-bot-action="save-one">save</button>
    </div>`);

  private get botCardEl(): Node {
    const botCard = frag<Element>(`<div class="bot-card">
        <div class="player"><span class="uid">${this.uid}</span></div>
      </div>`);

    buildFromSchema(this, ['info']);
    botCard.firstElementChild?.appendChild(this.panes.byId['info_description'].el);
    botCard.append(this.panes.byId['info_name'].el);
    botCard.append(this.panes.byId['info_ratings_classical'].el);
    botCard.append(this.botActionsEl);
    return botCard;
  }
}

interface ReadableBot extends BotInfo {
  readonly [key: string]: any;
}

interface WritableBot extends Bot {
  [key: string]: any;
  disabled: Set<string>;
}