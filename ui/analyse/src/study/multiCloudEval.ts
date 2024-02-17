import { Prop } from 'common';
import { EvalHitMulti } from '../interfaces';
import { storedBooleanPropWithEffect } from 'common/storage';
import { povChances } from 'ceval/src/winningChances';
import { ChapterPreview } from './interfaces';
import { bind, looseH as h } from 'common/snabbdom';
import { VNode } from 'snabbdom';

interface CloudEval extends EvalHitMulti {
  chances: number;
}
export type GetCloudEval = (preview: ChapterPreview) => CloudEval | undefined;

export class MultiCloudEval {
  showEval: Prop<boolean>;

  private cloudEvals: Map<Fen, CloudEval> = new Map();

  constructor(
    readonly redraw: () => void,
    private readonly send: SocketSend,
    private readonly variant: () => VariantKey,
    private readonly currentFens: () => Fen[],
  ) {
    this.showEval = storedBooleanPropWithEffect('analyse.multiboard.showEval', true, () => {
      this.redraw();
      this.sendRequest();
    });
  }

  sendRequest = () => {
    const fens = this.currentFens();
    if (fens.length && this.showEval())
      this.send('evalGetMulti', {
        fens,
        ...(this.variant() != 'standard' ? { variant: this.variant() } : {}),
      });
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

  getCloudEval = (preview: ChapterPreview): CloudEval | undefined => this.cloudEvals.get(preview.fen);
}

export const renderEvalToggle = (ctrl: MultiCloudEval): VNode =>
  h('input', {
    attrs: { type: 'checkbox', checked: ctrl.showEval() },
    hook: bind('change', e => ctrl.showEval((e.target as HTMLInputElement).checked)),
  });
