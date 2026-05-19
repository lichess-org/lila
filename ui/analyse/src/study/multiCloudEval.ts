import { type Prop, defined } from 'lib';
import { debounce } from 'lib/async';
import { povChances } from 'lib/ceval/winningChances';
import { storedBooleanPropWithEffect } from 'lib/storage';
import type { ClientEval, TreeNode } from 'lib/tree/types';

import type { EvalHitMulti } from '../interfaces';
import type { ServerNodeMsg } from './interfaces';
import type { StudyChapters } from './studyChapters';

export interface CloudEval extends EvalHitMulti {
  chances: number;
}
export type GetCloudEval = (fen: FEN) => CloudEval | undefined;

export class MultiCloudEval {
  showEval: Prop<boolean>;

  private readonly observed: Set<HTMLElement> = new Set();
  private readonly observer: IntersectionObserver | undefined =
    window.IntersectionObserver &&
    new IntersectionObserver(
      entries =>
        entries.forEach(entry => {
          const el = entry.target as HTMLElement;
          if (entry.isIntersecting) {
            this.observed.add(el);
            this.requestNewEvals();
          } else this.observed.delete(el);
        }),
      { threshold: 0.2 },
    );
  private readonly cloudEvals: Map<FEN, CloudEval> = new Map();

  constructor(
    readonly redraw: () => void,
    private readonly variant: () => VariantKey,
    private readonly chapters: StudyChapters,
    private readonly send: SocketSend,
  ) {
    this.showEval = storedBooleanPropWithEffect('analyse.multiboard.showEval', true, () => {
      this.redraw();
      this.requestNewEvals();
    });
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden) this.requestNewEvals();
    });
  }

  thisIfShowEval = (): MultiCloudEval | undefined => (this.showEval() ? this : undefined);

  observe = (el: HTMLElement) => this.observer?.observe(el);

  private readonly observedIds = () => new Set(Array.from(this.observed).map(el => el.dataset.id));

  private lastRequestedFens: Set<FEN> = new Set();

  private readonly sendRequestNow = () => {
    if (!this.showEval() || document.hidden) return;
    const ids = this.observedIds();
    const chapters = this.chapters
      .all()
      .filter(c => ids.has(c.id))
      .slice(0, 32);
    if (chapters.length) {
      const fensToRequest = new Set(chapters.map(c => c.fen));
      const alreadyHasAllFens = [...fensToRequest].every(f => this.lastRequestedFens.has(f));
      const worthSending = !alreadyHasAllFens || fensToRequest.size < this.lastRequestedFens.size / 1.5;
      if (worthSending) {
        this.lastRequestedFens = fensToRequest;
        const variant = this.variant(); // lila-ws only supports one variant for all fens
        this.send('evalGetMulti', {
          fens: Array.from(fensToRequest),
          ...(variant !== 'standard' ? { variant } : {}),
        });
      }
    }
  };

  private readonly requestNewEvals = debounce(this.sendRequestNow, 2000);

  onCloudEval = (d: EvalHitMulti) => {
    this.cloudEvals.set(d.fen, { ...d, chances: povChances('white', d) });
    this.redraw();
  };

  onLocalCeval = (node: TreeNode, ev: ClientEval) => {
    this.cloudEvals.set(node.fen, { ...ev, chances: povChances('white', ev) });
  };

  getCloudEval: GetCloudEval = (fen: FEN): CloudEval | undefined => this.cloudEvals.get(fen);

  addNode = (d: ServerNodeMsg) => {
    if (this.observedIds().has(d.p.chapterId)) this.requestNewEvals();
  };
}

export const renderScore = (s: EvalScore) =>
  s.mate ? '#' + s.mate : defined(s.cp) ? `${s.cp >= 0 ? '+' : ''}${s.cp / 100}` : '?';
