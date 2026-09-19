import * as co from 'chessops';
import { lichessRules } from 'chessops/compat';
import { setupPosition } from 'chessops/variant';

import { myUserId } from 'lib';
import { randomId } from 'lib/algo';
import type { CustomCeval } from 'lib/ceval/types';
import { mainlineNodeList, structuredCloneLite } from 'lib/tree/ops';
import type { ClientEval, Glyph, TreeNodeLite, TreePath } from 'lib/tree/types';

import AnalyseCtrl from '../ctrl';
import type { AnalysisUpdate, AnalysisEngineInfo, AnalysisMeta, Division } from '../interfaces';

// modules/tree/.../Analysis.scala
export interface ServerAnalysisDocument {
  id: string;
  studyId?: string;
  infos: { ply: number; eval: EvalScore & { best?: string }; variation: San[] }[];
  startPly: number;
  date: Date;
  engine: AnalysisEngineInfo;
}

export interface LocalAnalysisResult {
  localUpdate: AnalysisUpdate;
  serverDocument: ServerAnalysisDocument;
}

export class LocalAnalysisEngine {
  readonly nodes: TreeNodeLite[];
  busy = false;

  private readonly path: TreePath;
  private nodeIndex = 0;
  private nodesSearched = 0;
  private finishNode: (error?: 'cancelled') => void = () => {};

  constructor(private readonly ctrl: AnalyseCtrl) {
    this.ctrl.ceval.reset();
    this.nodes = mainlineNodeList(structuredCloneLite(this.ctrl.tree.root));
    for (const [i, node] of this.nodes.entries()) {
      node.eval = node.ceval = undefined;
      node.comments = undefined;
      node.glyphs = undefined;
      node.children = [this.nodes[i + 1]].filter(Boolean);
    }
    this.path = this.nodes.map(n => n.id).join('');
  }

  async getDivision(): Promise<Division> {
    const rsp = await fetch('/analysis/division', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        variant: this.ctrl.variantKey,
        initialFen: this.nodes[0].fen,
        moves: this.nodes.slice(1).map(n => n.san),
      }),
    });
    if (!rsp.ok) throw new Error(`${rsp.status} ${rsp.statusText} ${(await rsp.text()).slice(0, 255)}`);
    return rsp.json();
  }

  async analyse(
    custom: CustomCeval,
    division: Division,
    status: (moves: number, totalMoves: number, nodesPerMove: number) => void,
  ): Promise<LocalAnalysisResult> {
    try {
      this.busy = true;
      this.ctrl.initCeval({ emit: this.onEval, custom });
      while (this.isRunning()) {
        status(this.nodeIndex, this.nodes.length, this.nodesSearched / this.nodeIndex);
        await this.evaluateNode();
      }
      return this.review(division);
    } finally {
      this.busy = false;
      this.ctrl.initCeval();
    }
  }

  readonly stop = () => {
    this.ctrl.ceval.reset();
    this.finishNode(this.isRunning() ? 'cancelled' : undefined);
  };

  private readonly onEval = (ev: ClientEval | undefined) => {
    if (!ev) return this.stop();
    if (!this.isRunning()) return this.finishNode();
    if (ev.bestmove) {
      this.nodesSearched += ev.nodes;
      this.nodes[this.nodeIndex].eval = {
        cp: ev.cp,
        static: true,
        mate: ev.mate,
        best: ev.bestmove,
        fen: ev.fen,
        knodes: Math.round(ev.nodes / 1000),
        depth: ev.depth,
        pvs: ev.pvs.map(pv => ({ ...pv, moves: pv.moves.join(' ') })),
      };
      this.nodeIndex++;
      this.finishNode();
    }
  };

  private evaluateNode() {
    if (!this.isRunning()) return this.finishNode();

    this.nodes[this.nodeIndex].eval = undefined;
    this.ctrl.ceval.start(
      this.path.slice(0, this.nodeIndex * 2),
      this.nodes.slice(0, this.nodeIndex + 1),
      this.ctrl.idbTree.id,
    );

    return new Promise<void>((resolve, reject) => {
      this.finishNode = (error?: 'cancelled') => {
        if (error) reject(error);
        else resolve();
      };
    });
  }

  private async review(division: Division): Promise<LocalAnalysisResult> {
    const lines = this.nodes.slice(0, -1).map((parent, i) => {
      const chess = setupPosition(
        lichessRules(this.ctrl.variantKey),
        co.fen.parseFen(parent.fen).unwrap(),
      ).unwrap();
      const ucis = parent.eval!.pvs[0].moves.split(' ').slice(0, 12);
      if (ucis[0] === this.nodes[i + 1].uci) return { ucis: [], sans: [] };
      return {
        ucis,
        sans: ucis.map(uci => co.san.makeSanAndPlay(chess, co.parseUci(uci)!)),
      };
    });
    const nodesPerMove =
      'nodes' in this.ctrl.ceval.search.by
        ? this.ctrl.ceval.search.by.nodes
        : Math.round(
            this.nodes.reduce((sum, node) => sum + (node.eval?.knodes ?? 1000), 0) / this.nodes.length,
          ) * 1000;
    const engine = {
      id: String(this.ctrl.ceval.engines.active()!.id),
      nodesPerMove,
      userId: myUserId() ?? 'lichess',
      engineVersion: this.ctrl.ceval.engineVersion!,
    };
    const serverDocument: ServerAnalysisDocument = {
      id: this.ctrl.idbTree.id,
      studyId: this.ctrl.study?.data.id,
      infos: this.nodes.slice(1).map((node, i) => ({
        ply: node.ply,
        eval: { cp: node.eval?.cp, mate: node.eval?.mate, best: lines[i].ucis[0] },
        variation: lines[i].sans,
      })),
      startPly: this.nodes[0].ply,
      date: new Date(),
      engine,
    };
    const rsp = await fetch('/analysis/review', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ analysis: serverDocument, division }),
    });
    if (!rsp.ok) throw new Error(`${rsp.status} ${rsp.statusText}`);

    const review: Review = await rsp.json();
    review.moves.forEach((move, i) => {
      if (!move.judgment?.comment && !move.judgment?.glyph) {
        serverDocument.infos[i].eval.best = undefined;
        serverDocument.infos[i].variation = [];
        return;
      }
      const [parent, bad] = this.nodes.slice(i, i + 2);
      bad.comments = [{ id: randomId().slice(0, 4), by: 'lichess', comp: true, text: move.judgment.comment }];
      bad.glyphs = [{ ...move.judgment.glyph, comp: true }];
      parent.children.push(
        ucisToNodes(
          lines[i].ucis,
          setupPosition(lichessRules(this.ctrl.variantKey), co.fen.parseFen(parent.fen).unwrap()).unwrap(),
          bad.ply,
          { comp: true, clock: bad.clock, eval: undefined },
        )[0],
      );
    });

    return {
      localUpdate: {
        division,
        ch: this.ctrl.study?.data.chapter.id ?? '',
        tree: this.nodes[0],
        meta: {
          id: this.ctrl.idbTree.id,
          white: review.summary.white,
          black: review.summary.black,
          partial: false,
          engine,
        },
      },
      serverDocument,
    };
  }

  private isRunning() {
    return this.nodeIndex < this.nodes.length;
  }
}

function ucisToNodes(ucis: Uci[], initial: co.Position, ply: number, props: any): TreeNodeLite[] {
  const nodes: TreeNodeLite[] = [];
  const position = initial.clone();
  for (const [i, uci] of ucis.entries()) {
    const move = co.parseUci(uci)!;
    const san = co.san.makeSanAndPlay(position, move);
    nodes.push({
      id: co.compat.scalachessCharPair(move),
      ply: ply + i,
      fen: co.fen.makeFen(position.toSetup()),
      san,
      uci,
      ...props,
      children: [],
    });
    if (i > 0) nodes[i - 1].children?.push(nodes[i]);
  }
  return nodes;
}

type Review = {
  summary: AnalysisMeta;
  moves: { eval?: number; mate?: number; judgment?: { comment: string; glyph: Glyph } }[];
};
