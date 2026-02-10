import { domIdToUid, uidToDomId, botEquals } from './devBotCtrl';
import { handOfCards, type HandOfCards } from './handOfCards';
import { frag } from 'lib';
import { deepFreeze, definedMap } from 'lib/algo';
import { buildFromSchema, Panes } from './panes';
import { deadStrip } from './devUtil';
import { domDialog, type Dialog, type Action, confirm, alert } from 'lib/view';
import type { BotInfo } from 'lib/bot/types';
import { Bot } from 'lib/bot/bot';
import { AssetDialog, type AssetType } from './assetDialog';
import { historyDialog } from './historyDialog';
import { env } from './devEnv';
import { pubsub } from 'lib/pubsub';
import { Janitor } from 'lib/event';
import stringify from 'json-stringify-pretty-compact';
import * as licon from 'lib/licon';

export class EditDialog {
  static default: ReadableBot = deepFreeze<ReadableBot>({
    uid: '#default',
    name: '',
    description: '',
    vision: '',
    image: 'gray-torso.webp',
    books: [],
    fish: { multipv: 1, depth: 1 },
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
  scratch: Map<string, WritableBot> = new Map();
  janitor: Janitor = new Janitor();

  constructor(readonly color: Color) {
    this.view = frag<HTMLElement>($html`
      <div class="base-view dev-view edit-view with-cards">
        <div class="edit-bot"></div>
      </div>`);
    this.selectBot(localStorage.getItem('devBot.edit'));
    this.deck = handOfCards({
      viewEl: this.view,
      deckEl: this.view.querySelector('.placeholder') as HTMLElement,
      opaqueSelectedBackground: true,
      fanCenterToWidthRatio: 1 / 3,
      getCardData: () => this.cardData,
      getDrops: () => [{ el: this.view.querySelector('.player')!, selected: uidToDomId(this.editing().uid) }],
      select: (_, domId?: string) => this.selectBot(domIdToUid(domId)),
    });
  }

  async show(): Promise<Dialog> {
    this.dlg = await domDialog({
      append: [{ node: this.view }],
      actions: this.actions,
      noClickAway: true,
      onClose: () => this.janitor.cleanup(),
    });
    pubsub.on('botdev.import.book', this.onBookImported);
    this.janitor.addCleanupTask(() => pubsub.off('botdev.import.book', this.onBookImported));
    return this.dlg.show();
  }

  update(): void {
    this.dlg?.updateActions(this.actions);
    this.deck?.updateCards();
    this.view.querySelector('[data-bot-action="save-one"]')?.classList.toggle('none', !this.isDirty());
    this.view
      .querySelector('[data-bot-action="pull-one"]')
      ?.classList.toggle('none', !this.isDirty() && !this.localChanges);
    this.view
      .querySelector('[data-bot-action="push-one"]')
      ?.classList.toggle('none', !env.canPost || this.isDirty() || !this.localChanges);
  }

  assetDialog = async (type?: AssetType): Promise<string | undefined> => {
    this.assetDlg = new AssetDialog(type);
    const asset = await this.assetDlg.show();
    this.assetDlg = undefined;
    return asset;
  };

  editing(): WritableBot {
    let scratch = this.scratch.get(this.uid);
    if (!scratch) {
      scratch = Object.defineProperties(structuredClone(this.bots.get(this.uid) ?? {}), {
        disabled: { value: new Set<string>() },
        viewing: { value: new Map<string, string>() },
      }) as WritableBot;
      this.scratch.set(this.uid, scratch);
    }
    return scratch;
  }

  get localBot(): ReadableBot | undefined {
    return env.bot.localBots[this.uid];
  }

  get serverBot(): ReadableBot | undefined {
    return env.bot.serverBots[this.uid];
  }

  private get bots(): Map<string, Bot> {
    return env.bot.bots;
  }

  private get actions(): Action[] {
    return [
      ...this.panes.actions,
      { selector: '[data-bot-action="save-one"]', listener: () => this.save() },
      { selector: '[data-bot-action="new"]', listener: () => this.newBotDialog() },
      { selector: '[data-bot-action="vision"]', listener: () => this.visionDialog() },
      { selector: '[data-bot-action="history-one"]', listener: () => historyDialog(this, this.uid) },
      { selector: '[data-bot-action="json"]', listener: () => this.jsonDialog() },
      { selector: '[data-bot-action="unrate-all"]', listener: () => this.clearRatings() },
      { selector: '[data-bot-action="assets"]', listener: () => this.assetDialog() },
      { selector: '[data-bot-action="push-one"]', listener: () => this.push() },
      { selector: '[data-bot-action="pull-one"]', listener: () => this.pullBots([this.editing().uid]) },
      { selector: '[data-bot-action="pull-all"]', listener: () => this.pullBots() },
      { selector: '.player', listener: e => this.clickImage(e) },
    ];
  }

  private isDirty = (other: BotInfo | undefined = env.bot.info(this.uid)): boolean => {
    return (
      other !== undefined &&
      this.scratch.has(other.uid) &&
      !botEquals(other, deadStrip(this.scratch.get(other.uid)!)) // TODO avoid structured cloning for this
    );
  };

  private get localChanges(): boolean {
    return this.localBot !== undefined && !botEquals(this.localBot, this.serverBot);
  }

  private get cardData() {
    const speed = 'classical'; //env.game.speed;
    const all = [...new Map([...this.bots, ...this.scratch]).values()]; // scratches override bots
    return definedMap(all, b => env.bot.groupedCard(b, this.isDirty)).sort(env.bot.groupedSort(speed));
  }

  private async push() {
    const err = await env.push.pushBot(this.bots.get(this.uid)!);
    if (err) return alert(err);
    this.scratch.delete(this.uid);
    this.update();
  }

  private async save() {
    const behaviorScroll = this.view.querySelector('.behavior')!.scrollTop;
    const filtersScroll = this.view.querySelector('.filters')!.scrollTop;
    await env.bot.storeBot(deadStrip(this.editing()));
    this.update();
    this.view.querySelector('.behavior')!.scrollTop = behaviorScroll ?? 0;
    this.view.querySelector('.filters')!.scrollTop = filtersScroll ?? 0;
  }

  private selectBot(uid: string | null = this.uid): void {
    if (!this.bots.size) env.bot.storeBot(EditDialog.default);
    if (!uid || !this.bots.has(uid)) uid = env.bot[this.color]?.uid ?? env.bot.firstUid ?? '#default';
    this.uid = uid;
    localStorage.setItem('devBot.edit', uid);
    this.makeEditView();
    this.update();
  }

  private pullBots = async (uids?: string[]) => {
    if (!(await confirm(uids ? `Pull ${uids.join(' ')}?` : 'Pull all server bots?'))) return;
    const clear = (uids ?? Object.keys(this.bots)).filter(uid => env.bot.serverBots[uid]);
    clear.forEach(this.scratch.delete);
    await env.bot.clearStoredBots(clear);
    this.selectBot(this.editing().uid in this.bots ? this.editing().uid : Object.keys(this.bots)[0]);
  };

  private async clickImage(e: Event) {
    if (e.target !== e.currentTarget) return;
    const newImage = await this.assetDialog('image');
    if (!newImage) return;
    this.editing().image = newImage;
    this.update();
  }

  private makeEditView(): void {
    this.janitor?.cleanup();
    this.panes = new Panes();
    const el = this.view.querySelector('.edit-bot') as HTMLElement;
    el.innerHTML = '';
    el.appendChild(this.botCardEl);
    el.appendChild(buildFromSchema(this, ['behavior']).el);
    el.appendChild(buildFromSchema(this, ['bot_filters']).el);
    el.appendChild(this.deckEl);
    el.appendChild(this.globalActionsEl);
    this.panes.forEach(el => el.setEnabled());
  }

  private onBookImported = (key: string, oldKey?: string) => {
    this.assetDlg?.update();
    if (!oldKey) return;
    for (const bot of new Set<WritableBot>([this.editing(), ...Object.values(this.scratch)])) {
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

  private newBotDialog(): void {
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
      modal: true,
      actions: [
        {
          selector: 'input',
          event: ['input'],
          listener: () => {
            const newUid = input.value.toLowerCase();
            const isValid = /^#[a-z][a-z0-9-]{2,19}$/.test(newUid) && !this.bots.has(newUid);
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
            env.bot.storeBot({
              ...EditDialog.default,
              uid: input.dataset.uid,
              name: input.dataset.uid.slice(1),
            });
            this.selectBot(input.dataset.uid);
            dlg.close();
          },
        },
      ],
    }).then(dlg => {
      input.setSelectionRange(1, 1);
      dlg.show();
    });
  }

  private async visionDialog(): Promise<void> {
    const view = frag<HTMLElement>($html`
      <div class="dev-view">
        <p>A private description of who the bot is. This is only for the bot editor team, to know what the bot is about.</p>
        <textarea class="vision" rows="12">${this.editing().vision || ''}</textarea>
        <div class="actions">
          <button class="button button-empty button-red" data-action="cancel">cancel</button>
          <button class="button button-empty" data-action="save">save</button>
          </div>
      </div>`);
    const dlg = await domDialog({
      append: [{ node: view }],
      onClose: () => {},
      show: true,
      actions: [
        { selector: '[data-action="cancel"]', result: 'cancel' },
        { selector: '[data-action="save"]', result: 'save' },
      ],
    });
    if (dlg.returnValue !== 'save') return;
    this.editing().vision = view.querySelector<HTMLTextAreaElement>('.vision')!.value;
    this.makeEditView();
    this.update();
  }

  private async jsonDialog(): Promise<void> {
    const version = this.editing().version;
    const view = frag<HTMLElement>($html`
      <div class="dev-view json-dialog">
        <textarea class="json" autocomplete="false" spellcheck="false">${stringify(deadStrip(this.editing()), { indent: 2, maxLength: 80 })}</textarea>
        <div class="actions">
          <button class="button button-empty button-dim" data-icon="${licon.Clipboard}" data-action="copy"></button>
          <button class="button button-empty button-red" data-action="cancel">cancel</button>
          <button class="button button-empty" data-action="save">save</button>
          </div>
      </div>`);
    const dlg = await domDialog({
      append: [{ node: view }],
      onClose: () => {},
      show: true,
      actions: [
        { selector: '[data-action="cancel"]', result: 'cancel' },
        { selector: '[data-action="save"]', result: 'save' },
        {
          selector: '[data-action="copy"]',
          listener: async () => {
            await navigator.clipboard.writeText(view.querySelector<HTMLTextAreaElement>('.json')!.value);
            const copied = frag<HTMLElement>(
              `<div data-icon="${licon.Checkmark}" class="good"> COPIED</div>`,
            );
            view.querySelector('[data-action="copy"]')?.before(copied);
            setTimeout(() => copied.remove(), 2000);
          },
        },
      ],
    });
    if (dlg.returnValue !== 'save') return;

    const newBot = {
      ...(JSON.parse(view.querySelector<HTMLTextAreaElement>('.json')!.value) as BotInfo),
      version,
    };
    this.scratch.set(
      this.uid,
      Object.defineProperties(new Bot(newBot, env.bot), {
        disabled: { value: new Set<string>() },
        viewing: { value: new Map<string, string>() },
      }) as WritableBot,
    );
    this.makeEditView();
    this.update();
  }

  private deckEl = frag<HTMLElement>($html`
    <div class="deck">
      <div class="placeholder"></div>
      <fieldset class="deck-legend">
        <legend>legend</legend>
        <label class="clean dirty">dirty</label>
        <label class="local-only">unshared</label>
        <label class="local-changes">local changes</label>
        <label class="upstream-changes">upstream changes</label>
      </fieldset>
    </div>`);

  private globalActionsEl = frag<HTMLElement>($html`
    <div class="global-actions">
      <button class="button button-empty button-green" data-bot-action="new">new bot</button>
      <button class="button button-empty button-brag" data-bot-action="assets">assets</button>
      <button class="button button-empty" data-bot-action="pull-all">pull all</button>
      <button class="button button-empty button-red" data-bot-action="unrate-all">clear all ratings</button>
    </div>`);

  private botActionsEl = frag<HTMLElement>($html`
    <div class="bot-actions">
      <button class="button button-empty button" data-bot-action="vision">vision</button>
      <button class="button button-empty button-dim" data-bot-action="json">json</button>
      <button class="button button-empty button-dim" data-bot-action="history-one">history</button>
      <button class="button button-empty none" data-bot-action="pull-one">pull</button>
      <button class="button button-empty button-clas none" data-bot-action="push-one">push</button>
      <button class="button none" data-bot-action="save-one">save</button>
    </div>`);

  private get botCardEl(): Node {
    const botCard = frag<Element>($html`
      <div class="bot-card">
        <div class="player"><span class="uid">${this.uid}</span></div>
      </div>`);

    buildFromSchema(this, ['info']);
    botCard.firstElementChild?.appendChild(this.panes.byId['info_description'].el);
    botCard.append(this.botActionsEl);
    const underBot = frag<HTMLElement>('<div class="under-bot"></div>');
    underBot.append(this.panes.byId['info_name'].el);
    underBot.append(this.panes.byId['info_ratings'].el);
    botCard.append(underBot);
    return botCard;
  }
}

interface ReadableBot extends BotInfo {
  readonly [key: string]: any;
}

interface WritableBot extends Bot {
  [key: string]: any;
  disabled: Set<string>;
  viewing: Map<string, string>;
}
