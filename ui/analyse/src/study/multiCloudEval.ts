import { Prop, defined } from 'common';
import { EvalHitMulti } from '../interfaces';
import { storedBooleanPropWithEffect } from 'common/storage';
import { povChances } from 'ceval/src/winningChances';
import { bind, looseH as h } from 'common/snabbdom';
import { VNode } from 'snabbdom';
import { FEN } from 'chessground/types';
import { ChapterId } from './interfaces';
import { StudyChapters } from './studyChapters';

interface CloudEval extends EvalHitMulti {
  chances: number;
}
export type GetCloudEval = (fen: FEN) => CloudEval | undefined;

export class MultiCloudEval {
  showEval: Prop<boolean>;

  private observed: Set<ChapterId> = new Set();
  private observer = new IntersectionObserver(
    entries =>
      entries.forEach(entry => {
        const id = (entry.target as HTMLElement).dataset['id']!;
        if (entry.isIntersecting) this.observed.add(id);
        else this.observed.delete(id);
        console.log(this.observed.size);
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
      this.sendRequest();
    });

    setInterval(() => console.log(this.observed), 2000);
  }

  thisIfShowEval = (): MultiCloudEval | undefined => (this.showEval() ? this : undefined);

  observe = (el: HTMLElement) => this.observer.observe(el);

  sendRequest = () => {
    const chapters = this.chapters.all().filter(c => this.observed.has(c.id));
    if (chapters.length && this.showEval()) {
      const variant = chapters[0].variant; // lila-ws only supports one variant for all fens
      this.send('evalGetMulti', {
        fens: chapters.map(c => c.fen),
        ...(variant != 'standard' ? { variant } : {}),
      });
    }
  };

  onCloudEval = (d: EvalHitMulti) => {
    this.cloudEvals.set(d.fen, { ...d, chances: povChances('white', d) });
    this.redraw();
  };

  onLocalCeval = (node: Tree.Node, ev: Tree.ClientEval) => {
    const cur = this.cloudEvals.get(node.fen);
    if (!cur || cur.depth < ev.depth)
      this.cloudEvals.set(node.fen, { ...ev, chances: povChances('white', ev) });
  };

  getCloudEval: GetCloudEval = (fen: FEN): CloudEval | undefined => this.cloudEvals.get(fen);
}

export const renderEvalToggle = (ctrl: MultiCloudEval): VNode =>
  h('input', {
    attrs: { type: 'checkbox', checked: ctrl.showEval() },
    hook: bind('change', e => ctrl.showEval((e.target as HTMLInputElement).checked)),
  });

export const renderScore = (s: EvalScore) =>
  s.mate ? '#' + s.mate : defined(s.cp) ? `${s.cp >= 0 ? '+' : ''}${s.cp / 100}` : '?';
