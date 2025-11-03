import { myUserId } from 'lib';
import type { CustomCeval } from 'lib/ceval/types';
import { mainlineNodeList, structuredCloneLite } from 'lib/tree/ops';
import type { ClientEval, TreeNodeLite, TreePath } from 'lib/tree/types';

import { isFinished } from '@/study/studyChapters';

import type AnalyseCtrl from '../ctrl';
import type { StaticAnalysisData } from '../interfaces';
import { accuracy } from './accuracy';
import { annotate } from './annotate';
import { dividePhases } from './dividePhases';

// modules/tree/.../Analysis.scala
export interface ServerAnalysisDocument {
  id: string;
  studyId?: string;
  infos: { ply: number; eval: EvalScore; line?: San[] }[];
  startPly: number;
  date: Date;
  engine: AnalysisEngineInfo;
}

export interface LocalAnalysisResult {
  localAnalysis: StaticAnalysisData;
  serverDocument: ServerAnalysisDocument;
}

export interface AnalysisEngineInfo {
  id: string; // engine id
  userId: string;
  nodesPerMove: number;
}

export interface PostResult {
  status: 'ok' | 'conflict' | 'locked' | 'error';
  errorText?: string;
}

export async function uploadAnalysis(serverDocument: ServerAnalysisDocument): Promise<PostResult> {
  const rsp = await fetch('/analysis/local', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(serverDocument),
  });
  const result: PostResult = {
    status: rsp.ok ? 'ok' : rsp.status === 409 ? 'conflict' : rsp.status === 423 ? 'locked' : 'error',
  };
  if (!rsp.ok && rsp.status !== 409) {
    result.errorText = `${rsp.status} ${rsp.statusText} ${(await rsp.text()).slice(0, 255)}`;
  }
  return result;
}

export function canLocalAnalyse(ctrl: AnalyseCtrl): boolean {
  return !ctrl.ongoing && ctrl.mainline.length > 5 && (!ctrl.study || ctrl.study.isCevalAllowed());
}

type CanUploadAnalysis = {
  allowed: boolean;
  reason?: 'rec' | 'permission' | 'invalid' | 'ongoing' | string;
};

export function canUploadAnalysis(ctrl: AnalyseCtrl): CanUploadAnalysis {
  if (!canLocalAnalyse(ctrl)) return { allowed: false };
  if (!myUserId()) return { allowed: false, reason: 'permission' };
  if (ctrl.study && !ctrl.study.members.canContribute()) return { allowed: false, reason: 'permission' };
  if (ctrl.mainline.length < 10 || !ctrl.ceval.analysable) return { allowed: false, reason: 'invalid' };
  if (ctrl.study && !ctrl.study?.vm.mode.write) return { allowed: false, reason: 'rec' };
  if (ctrl.ongoing || (ctrl.study?.relay && !isFinished(ctrl.study.data.chapter)))
    return { allowed: false, reason: 'ongoing' };
  if (ctrl.synthetic && !ctrl.study) return { allowed: false };
  if (!ctrl.allowLines()) return { allowed: false };
  return { allowed: true };
}

export class LocalAnalysisEngine {
  nodes: TreeNodeLite[];
  private readonly path: TreePath;
  private nodeIndex = 0;
  private nodesSearched = 0;
  private finishNode: (error?: 'cancelled') => void;

  constructor(
    private readonly ctrl: AnalyseCtrl,
    private readonly status: (moves: number, totalMoves: number, nodesPerMove: number) => void,
    private readonly notify: (nodes: TreeNodeLite[]) => void,
  ) {
    this.ctrl.ceval.reset();
    this.nodes = mainlineNodeList(structuredCloneLite(this.ctrl.tree.root));
    for (const [i, node] of this.nodes.entries()) {
      node.eval = undefined;
      node.comments = undefined;
      node.children = [this.nodes[i + 1]].filter(Boolean);
    }
    this.path = this.nodes.map(n => n.id).join('');
  }

  async analyse(custom: CustomCeval): Promise<LocalAnalysisResult> {
    try {
      this.ctrl.initCeval({ emit: this.onEval, custom });
      this.ctrl.data.game.division = dividePhases(this.nodes);
      this.notify(this.nodes);
      while (this.isRunning()) {
        await this.evaluateNode();
      }
      return this.review();
    } finally {
      this.ctrl.initCeval();
    }
  }

  readonly stop = () => {
    this.ctrl.ceval.reset();
    this.finishNode(this.isRunning() ? 'cancelled' : undefined);
  };

  private readonly onEval = (ev: ClientEval) => {
    if (!this.isRunning()) return this.finishNode();
    if (ev.bestmove) {
      this.nodesSearched += ev.nodes;
      this.nodes[this.nodeIndex].eval = {
        cp: ev.cp,
        mate: ev.mate,
        best: ev.bestmove,
        fen: ev.fen,
        knodes: Math.round(ev.nodes / 1000),
        depth: ev.depth,
        pvs: ev.pvs.map(pv => ({ ...pv, moves: pv.moves.join(' ') })),
      };
      this.notify(this.nodes);
      this.nodeIndex++;
      this.finishNode();
    }
  };

  private evaluateNode() {
    if (!this.isRunning()) return this.finishNode();

    this.status(this.nodeIndex, this.nodes.length, this.nodesSearched / this.nodeIndex);

    const nodeSlice = this.nodeIndex + 1;
    this.nodes[this.nodeIndex].eval = undefined;
    this.ctrl.ceval.start(this.path.slice(0, nodeSlice * 2), this.nodes.slice(0, nodeSlice), undefined);

    return new Promise<void>((resolve, reject) => {
      this.finishNode = (error?: 'cancelled') => {
        if (error) reject(error);
        else resolve();
      };
    });
  }

  private review() {
    const { totals, infos } = annotate(this.nodes);
    const { white, black } = accuracy(this.nodes);
    const nodesPerMove =
      'nodes' in this.ctrl.ceval.search.by
        ? this.ctrl.ceval.search.by.nodes
        : Math.round(
            this.nodes.reduce((sum, node) => sum + (node.eval?.knodes ?? 1000), 0) / this.nodes.length,
          ) * 1000;
    const engine = {
      id: String(this.ctrl.ceval.engines.active().id),
      nodesPerMove,
      userId: myUserId()!,
    };
    return {
      localAnalysis: {
        tree: this.nodes[0],
        division: { ...this.ctrl.data.game.division },
        ch: this.ctrl.study?.currentChapter().id ?? '',
        analysis: {
          id: this.ctrl.data.game.id,
          white: { ...totals.white, accuracy: white.percent, acpl: white.acpl },
          black: { ...totals.black, accuracy: black.percent, acpl: black.acpl },
          partial: false,
        },
        engine,
      },
      serverDocument: {
        id: this.ctrl.study?.data.chapter.id ?? this.ctrl.data.game.id,
        studyId: this.ctrl.study?.data.id,
        infos: infos.map((mv, i) => ({
          ply: this.nodes[1].ply + i,
          eval: { cp: mv.cp, mate: mv.mate, best: mv.best },
          variation: mv.line ?? [],
        })),
        startPly: this.nodes[0].ply,
        date: new Date(),
        engine,
      },
    };
  }

  private isRunning() {
    return this.nodeIndex < this.nodes.length;
  }
}
