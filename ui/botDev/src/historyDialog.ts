import { domDialog, type Dialog } from 'lib/view/dialog';
import { frag, escapeHtml, myUserId } from 'lib';
import * as licon from 'lib/licon';
import type { EditDialog } from './editDialog';
import { env } from './devEnv';
import type { BotInfo } from 'lib/bot/types';
import stringify from 'json-stringify-pretty-compact';
import diff from 'fast-diff';

interface BotVersionInfo extends Omit<BotInfo, 'version'> {
  author: string;
  version: string | number;
}

export async function historyDialog(host: EditDialog, uid: string): Promise<void> {
  const dlg = new HistoryDialog(host, uid);
  await dlg.show();
}

class HistoryDialog {
  dlg: Dialog;
  view: HTMLElement;
  versions: BotVersionInfo[];

  constructor(
    readonly host: EditDialog,
    readonly uid: string,
  ) {}

  async show(): Promise<this> {
    this.view = frag<HTMLElement>(`<div class="dev-view history-dialog">
        <div class="versions"></div>
        <div class="json"></div>
        <div class="actions">
          <button class="button button-empty" data-action="pull">pull</button>
          <button class="button button-empty button-clas" data-action="push">push</button>
        </div>
        <div class="actions">
          <button class="button button-empty button-dim" data-icon="${licon.Clipboard}" data-action="copy"></button>
        </div>
      </div>`);
    await this.updateHistory();
    this.dlg = await domDialog({
      append: [{ node: this.view }],
      onClose: () => {},
      actions: [
        { selector: '[data-action="pull"]', listener: this.pull },
        { selector: '[data-action="push"]', listener: this.push },
        { selector: '.version', listener: this.clickItem },
        { selector: '.version', event: 'mouseenter', listener: this.mouseEnterItem },
        { selector: '.version', event: 'mouseleave', listener: () => this.json() },
        { selector: '[data-action="copy"]', listener: this.copy },
      ],
    });
    this.select(this.versions[this.versions.length - 1]);
    this.dlg.show();
    const versionsEl = this.view.querySelector('.versions') as HTMLElement;
    versionsEl.scrollTop = versionsEl.scrollHeight;
    return this;
  }

  async updateHistory() {
    const history = await (await fetch('/bots/dev/history?id=' + encodeURIComponent(this.uid))).json();
    this.versions = history.bots.reverse();
    if (env.bot.localBots[this.uid])
      this.versions.push({
        ...env.bot.localBots[this.uid],
        version: 'local',
        author: myUserId() ?? 'anonymous',
      });
    const versionsEl = this.view.querySelector('.versions') as HTMLElement;
    versionsEl.innerHTML = '';
    for (const bot of this.versions) {
      const isLive =
        bot === env.bot.localBots[this.host.uid] || bot === this.versions[this.versions.length - 1];
      const version = bot.version;
      const div = frag<HTMLElement>(
        `<div class="version${isLive ? ' selected' : ''}" data-version="${version}">`,
      );
      const versionStr = typeof version === 'number' ? `#${version}` : version;
      const span = frag(`<span class="author">${bot.author}</span>`);
      if (isLive) span.appendChild(frag(`<i data-icon="${licon.Checkmark}" class="live">`));
      div.append(frag(`<span class="version-number">${versionStr}</span>`), span);
      versionsEl.append(div);
    }
    versionsEl.scrollTop = versionsEl.scrollHeight;
    this.dlg?.updateActions();
  }

  clickItem = async (e: Event) => {
    this.select(this.version((e.target as HTMLElement).dataset.version));
  };

  mouseEnterItem = async (e: Event) => {
    this.json(this.version((e.target as HTMLElement)?.dataset.version));
  };

  select(bot: BotVersionInfo | undefined = this.selected): void {
    this.view.querySelector('[data-action="pull"]')?.classList.toggle('none', bot && bot === this.live);
    this.view
      .querySelector('[data-action="push"]')
      ?.classList.toggle('none', !env.canPost || bot?.version !== 'local');
    if (!bot) return;
    this.view.querySelectorAll('.version')?.forEach(v => v.classList.remove('selected'));
    this.versionEl(bot.version)?.classList.add('selected');
    this.json(bot);
  }

  version(version: string | number | undefined): BotVersionInfo | undefined {
    if (!version) return;
    return this.versions.find(b => String(b.version) === String(version));
  }

  versionEl(version: string | number): HTMLElement | undefined {
    return this.view.querySelector(`.version[data-version="${version}"]`) as HTMLElement;
  }

  copy = async () => {
    await navigator.clipboard.writeText(stringify(this.selected!));
    const copied = frag<HTMLElement>(`<div data-icon="${licon.Checkmark}" class="good"> COPIED</div>`);
    this.view.querySelector('[data-action="copy"]')?.before(copied);
    setTimeout(() => copied.remove(), 2000);
  };

  pull = async () => {
    await env.bot.storeBot(this.selected as BotInfo);
    await this.updateHistory();
    this.select();
    this.host.update();
  };

  push = async () => {
    const err = await env.push.pushBot(this.selected as BotInfo);
    if (err) {
      alert(`push failed: ${escapeHtml(err)}`);
      return;
    }
    await this.updateHistory();
    this.select();
    this.host.update();
  };

  get selected() {
    const selected = this.view.querySelector('.version.selected') as HTMLElement;
    return selected && this.versions.find(b => String(b.version) === selected.dataset.version);
  }

  get live() {
    return this.versions[this.versions.length - 1];
  }

  json(hover?: BotVersionInfo) {
    const json = this.view.querySelector('.json') as HTMLElement;
    json.innerHTML = '';
    const changes = diff(
      stringify(this.selected, { indent: 2, maxLength: 80 }),
      stringify(hover ?? this.selected, { indent: 2, maxLength: 80 }),
    );
    for (const change of changes) {
      const span = frag<HTMLElement>(`<span>${change[1]}</span>`);
      if (change[0] === 1) span.classList.add('hovered');
      else if (change[0] === -1) span.classList.add('selected');
      json.append(span);
    }
  }
}
