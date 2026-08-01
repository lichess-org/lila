import { myUsername } from 'lib';
import { deepFreeze, randomToken, clamp } from 'lib/algo';
import { displayColumns } from 'lib/device';
import { log } from 'lib/permalog';
import type { ClockConfig } from 'lib/setup/interfaces';
import type { LobbyShortcut } from 'lib/types';
import { alert } from 'lib/view';
import { jsonSimple } from 'lib/xhr';

import type LobbyController from './ctrl';
import type { Pool } from './interfaces';

export const pools = deepFreeze([
  // mirrors modules/pool/src/main/PoolList.scala
  { id: '1+0', lim: 1, inc: 0, perf: i18n.site.bullet },
  { id: '2+1', lim: 2, inc: 1, perf: i18n.site.bullet },
  { id: '3+0', lim: 3, inc: 0, perf: i18n.site.blitz },
  { id: '3+2', lim: 3, inc: 2, perf: i18n.site.blitz },
  { id: '5+0', lim: 5, inc: 0, perf: i18n.site.blitz },
  { id: '5+3', lim: 5, inc: 3, perf: i18n.site.blitz },
  { id: '10+0', lim: 10, inc: 0, perf: i18n.site.rapid },
  { id: '10+5', lim: 10, inc: 5, perf: i18n.site.rapid },
  { id: '15+10', lim: 15, inc: 10, perf: i18n.site.rapid },
  { id: '30+0', lim: 30, inc: 0, perf: i18n.site.classical },
  { id: '30+20', lim: 30, inc: 20, perf: i18n.site.classical },
] as const satisfies readonly Pool[]);

export const siteShortcuts = deepFreeze([
  { id: 'myGames', iconKey: 'Multiboard', name: 'My games', url: `/@/${myUsername()}/all#angles` },
  { id: 'gameSearch', iconKey: 'Search', name: 'Game search', url: '/games/search' },
  { id: 'openings', iconKey: 'Book', name: 'Openings', url: '/opening' },
  { id: 'communityBlogs', iconKey: 'InkQuill', name: 'Community blogs', url: '/blog/community' },
  { id: 'learnBasics', iconKey: 'GraduateCap', name: 'Learn the basics', url: '/learn' },
  { id: 'practice', iconKey: 'GraduateCap', name: 'Practice', url: '/practice' },
  { id: 'coordinates', iconKey: 'Move', name: 'Coordinates trainer', url: '/training/coordinate' },
  { id: 'class', iconKey: 'GraduateCap', name: 'Class', url: '/class' },
  { id: 'broadcasts', iconKey: 'RadioTower', name: 'Broadcasts', url: '/broadcast' },
  { id: 'fidePlayers', iconKey: 'User', name: 'FIDE players', url: '/fide' },
  { id: 'tournaments', iconKey: 'Trophy', name: 'Tournaments', url: '/tournament' },
  { id: 'leaderboard', iconKey: 'User', name: 'Leaderboard', url: '/player' },
  { id: 'puzzleStorm', iconKey: 'Storm', name: 'Puzzle storm', url: '/storm' },
  { id: 'puzzleStreak', iconKey: 'ArrowThruApple', name: 'Puzzle streak', url: '/streak' },
  {
    id: 'bots',
    iconMaskUrl: site.asset.url('images/icons/robot.svg'),
    name: 'Bots',
    url: '/player/bots',
  },
  {
    id: 'puzzleRacer',
    iconUrl: site.asset.url('images/racer/checkered-flag.svg'),
    name: 'Puzzle racer',
    url: '/racer',
  },
  {
    id: 'offTopic',
    iconKey: 'BubbleConvo',
    name: 'Off-topic discussion',
    url: '/forum/off-topic-discussion',
  },
  {
    id: 'chessDiscussion',
    iconKey: 'BubbleConvo',
    name: 'Chess discussion',
    url: '/forum/general-chess-discussion',
  },
  {
    id: 'analysisDiscussion',
    iconKey: 'BubbleConvo',
    name: 'Analysis discussion',
    url: '/forum/game-analysis',
  },
  { id: 'feedback', iconKey: 'Tools', name: 'Feedback & support', url: '/forum/lichess-feedback' },
] as const satisfies readonly LobbyShortcut[]);

const slotCount = 3 * 4 - 1; // minus 1 for customize button

type ConfiguredShortcuts = (Shortcut | null)[];
type Shortcut = LobbyShortcut & Partial<Pool> & { hidden?: boolean; static?: boolean };

export class ShortcutsCtrl {
  loaded: Promise<void>;
  private slots = Array<string | null>(slotCount).fill(null);
  private readonly all = new Map<string, Shortcut>([
    ['customize', { id: 'customize', name: 'Customize', iconKey: 'StarOutline', hidden: true }],
  ]);

  constructor(
    private readonly ctrl?: LobbyController,
    private contextual?: Partial<LobbyShortcut>[],
  ) {
    if (ctrl) {
      this.init(ctrl.opts.lobbyShortcuts);
      this.loaded = Promise.resolve();
    } else {
      this.loaded = new Promise<void>(resolve =>
        jsonSimple('/account/pref-json/lobbyShortcuts')
          .catch(() => undefined)
          .then((userSlots?: ConfiguredShortcuts) => {
            this.init(userSlots);
            resolve();
          }),
      );
    }
  }

  get scratch(): LobbyShortcut[] {
    return [...this.all.entries()]
      .filter(([id, val]) => !val.hidden && !this.slots.includes(id))
      .map(([, val]) => val)
      .sort(this.comparator);
  }

  get configured(): ConfiguredShortcuts {
    return this.slots.map(this.get);
  }

  get = (id: string | null): LobbyShortcut | null => (id && this.all.get(id)) || null;

  reset(): void {
    this.setConfigured(undefined);
  }

  async save(): Promise<void> {
    const trimmed = this.configured.map(s => {
      if (!s) return null;
      if (s.static) return { id: s.id };
      return Object.fromEntries(
        Object.entries(s).filter(([, v]) => v !== undefined && typeof v !== 'boolean'),
      );
    });
    try {
      const postUrl = '/account/pref-json/lobbyShortcuts';
      const rsp = await fetch(postUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(trimmed),
      });
      if (!rsp.ok) {
        log(`POST ${postUrl} ${rsp.status}: ${(await rsp.text()).slice(0, 200)}`);
        throw new Error();
      }
      this.ctrl?.redraw();
    } catch {
      alert(`Failed to save lobby shortcuts. Try again later`);
    }
  }

  place(id: string, index: number): boolean {
    if (!Number.isInteger(index) || !this.all.has(id)) return false;
    index = clamp(index, { min: 0, max: this.slots.length - 1 });
    const from = this.slots.indexOf(id);
    if (from < 0) this.slots[index] = id;
    else [this.slots[from], this.slots[index]] = [this.slots[index], this.slots[from]];
    return true;
  }

  remove(id: string): boolean {
    const index = this.slots.indexOf(id);
    if (index < 0) return false;
    this.slots[index] = null;
    return true;
  }

  onclick(id: string): void {
    const shortcut = this.all.get(id);
    if (!shortcut) return;
    if (pools.some(p => p.id === shortcut.id)) this.ctrl?.clickPool(shortcut.id);
    else if (shortcut.id === 'customize')
      site.asset.loadEsm('lobby.shortcutsDialog', { init: { ctrl: this } });
    else if (shortcut.url) site.redirect(shortcut.url);
    else alert('What do? ' + JSON.stringify(shortcut));
  }

  scratchIndexOf(id: string): number {
    const shortcut = this.all.get(id);
    if (!shortcut) return NaN;
    const index = this.scratch.findIndex(s => this.comparator(shortcut, s) <= 0);
    return index < 0 ? this.scratch.length : index;
  }

  configuredIndexOf(id: string): number {
    const maybeNegative = this.slots.indexOf(id);
    return maybeNegative < 0 ? NaN : maybeNegative;
  }

  setup(contextual?: LobbyShortcut[]): void {
    this.contextual?.forEach(s => this.all.delete(s.id!));
    this.contextual = contextual;
    this.init(this.configured);
  }

  private init(userSlots?: ConfiguredShortcuts) {
    const isHidden = (id: string) => Boolean(this.contextual) && !userSlots?.some(s => s?.id === id);
    siteShortcuts.forEach(s => this.all.set(s.id, { ...s, static: true, hidden: isHidden(s.id) }));
    pools.forEach(p => this.all.set(p.id, { ...p, name: p.perf, static: true, hidden: isHidden(p.id) }));

    userSlots?.forEach(this.add);
    this.setConfigured(userSlots);

    this.contextual?.forEach(shortcut => {
      const s = structuredClone(shortcut);
      s.id ??= [...this.all.values()].find(v => s.name === v.name && s.url === v.url)?.id ?? randomToken();
      this.add(s as Shortcut);
    });
  }

  private readonly add = (shortcut: Shortcut | null) => {
    if (!shortcut || this.all.has(shortcut.id)) return;
    this.all.set(shortcut.id, shortcut);
  };

  private setConfigured<T extends { id: string }>(slots: readonly (string | T | null)[] | undefined) {
    const ids = (slots ?? pools).map(s => (typeof s === 'string' ? s : (s?.id ?? null)));
    for (const s of ids.filter(Boolean).map(id => this.all.get(id!))) {
      if (s) s.hidden = false;
    }
    this.slots = [...ids.slice(0, slotCount), ...Array(Math.max(0, slotCount - ids.length)).fill(null)];
  }

  private readonly comparator = (a: Shortcut, b: Shortcut) => {
    if (!a || !b) return !a && !b ? 0 : !a ? -1 : 1;
    const aClock = 'lim' in a && 'inc' in a && (a as ClockConfig);
    const bClock = 'lim' in b && 'inc' in b && (b as ClockConfig);
    if (aClock && bClock) return aClock.lim - bClock.lim || aClock.inc - bClock.inc;
    if (aClock) return 1;
    if (bClock) return -1;
    return (a.name ?? a.id).localeCompare(b.name ?? b.id);
  };
}

export function fitShortcut(
  s: Shortcut,
  effectiveLengthTarget = 32,
  truncateAt = 80,
): { scale: number; text: string } {
  if ('perf' in s) return { scale: 1, text: s.id };

  //
  const scaleBy = 0.9;
  const minEm = 0.65;

  let text = s.name ?? s.id;

  if (text.length > truncateAt) {
    text = text.slice(0, truncateAt / 2 - 2) + ' ... ' + text.slice(text.length - truncateAt / 2 - 3);
  }
  let scale = (displayColumns() === 1 && text.length > 8) || text.split(' ').length > 1 ? scaleBy : 1;
  let effectiveLength = text.length;
  while (scale > minEm && effectiveLength > effectiveLengthTarget) {
    scale *= scaleBy;
    effectiveLength *= scaleBy;
  }
  return { scale, text };
}
