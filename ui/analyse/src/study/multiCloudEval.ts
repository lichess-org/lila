import { type Prop, defined } from 'lib';
import type { EvalHitMulti } from '../interfaces';
import { storedBooleanPropWithEffect } from 'lib/storage';
import { povChances } from 'lib/ceval/winningChances';
import { type VNode, bind, hl } from 'lib/view';
import type { StudyChapters } from './studyChapters';
import { debounce } from 'lib/async';
import type { ServerNodeMsg } from './interfaces';
import type { ClientEval, TreeNode } from 'lib/tree/types';

export interface CloudEval extends EvalHitMulti {
  chances: number;
}
export type GetCloudEval = (fen: FEN) => CloudEval | undefined;

export class MultiCloudEval {
  showEval: Prop<boolean>;

  private observed: Set<HTMLElement> = new Set();
  private observer: IntersectionObserver | undefined =
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
  private cloudEvals: Map<FEN, CloudEval> = new Map();

  constructor(
    readonly redraw: () => void,
    private readonly chapters: StudyChapters,
    private readonly send: SocketSend,
  ) {
    this.showEval = storedBooleanPropWithEffect('analyse.multiboard.showEval', true, () => {
      this.redraw();
      this.requestNewEvals();
    });
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible') this.requestNewEvals();
    });
  }

  thisIfShowEval = (): MultiCloudEval | undefined => (this.showEval() ? this : undefined);

  observe = (el: HTMLElement) => this.observer?.observe(el);

  private observedIds = () => new Set(Array.from(this.observed).map(el => el.dataset.id));

  private lastRequestedFens: Set<FEN> = new Set();

  private sendRequestNow = () => {
    if (!this.showEval() || document.visibilityState === 'hidden') return;
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
        const variant = chapters[0].variant; // lila-ws only supports one variant for all fens
        this.send('evalGetMulti', {
          fens: Array.from(fensToRequest),
          ...(variant !== 'standard' ? { variant } : {}),
        });
      }
    }
  };

  private requestNewEvals = debounce(this.sendRequestNow, 2000);

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

export const renderEvalToggle = (ctrl: MultiCloudEval): VNode =>
  hl('input', {
    attrs: { type: 'checkbox', checked: ctrl.showEval() },
    hook: bind('change', e => ctrl.showEval((e.target as HTMLInputElement).checked)),
  });

export const renderScore = (s: EvalScore) =>
  s.mate ? '#' + s.mate : defined(s.cp) ? `${s.cp >= 0 ? '+' : ''}${s.cp / 100}` : '?';
