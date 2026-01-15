import { ops as treeOps } from 'lib/tree/tree';
import type AnalyseCtrl from './ctrl';
import type { EvalGetData, EvalPutData, Opening, ServerEvalData } from './interfaces';
import type { AnaDrop, AnaMove, ChapterData, EditChapterData } from './study/interfaces';
import type { FormData as StudyFormData } from './study/studyForm';

interface MoveOpts {
  write?: false;
  sticky?: false;
}

export interface ReqPosition {
  ch: string;
  path: string;
}

interface GameUpdate {
  id: string;
  fen: FEN;
  lm: Uci;
  wc?: number;
  bc?: number;
}

export interface StudySocketSendParams {
  setPath: (d: ReqPosition) => void;
  deleteNode: (d: ReqPosition & { jumpTo: string }) => void;
  promote: (d: ReqPosition & { toMainline: boolean }) => void;
  forceVariation: (d: ReqPosition & { force: boolean }) => void;
  shapes: (d: ReqPosition & { shapes: Tree.Shape[] }) => void;
  setComment: (d: ReqPosition & { id?: string; text: string }) => void;
  deleteComment: (d: ReqPosition & { id: string }) => void;
  setGamebook: (d: ReqPosition & { gamebook: { deviation?: string; hint?: string } }) => void;
  toggleGlyph: (d: ReqPosition & { id: number }) => void;
  explorerGame: (d: ReqPosition & { gameId: string; insert: boolean }) => void;
  setChapter: (chapterId: string) => void;
  setRole: (d: { userId: string; role: string }) => void;
  addChapter: (d: ChapterData & { sticky?: boolean; showRatings?: boolean }) => void;
  editChapter: (d: EditChapterData) => void;
  descStudy: (desc: string) => void;
  descChapter: (d: { id: string; desc: string }) => void;
  deleteChapter: (chapterId: string) => void;
  clearAnnotations: (chapterId: string) => void;
  clearVariations: (chapterId: string) => void;
  sortChapters: (chapterIds: string[]) => void;
  setTag: (d: { chapterId: string; name: string; value: string }) => void;
  anaMove: (d: AnaMove & MoveOpts) => void;
  anaDrop: (d: AnaDrop & MoveOpts) => void;
  opening: (d: { fen: FEN }) => void;
  like: (d: { liked: boolean }) => void;
  kick: (username: string) => void;
  editStudy: (d: StudyFormData) => void;
  setTopics: (topics: string[]) => void;
  requestAnalysis: (chapterId: string) => void;
  invite: (username: string) => void;
  relaySync: (sync: boolean) => void;
  leave: () => void;
}

export interface EvalCacheSocketParams {
  evalPut: (d: EvalPutData) => void;
  evalGet: (d: EvalGetData) => void;
}

export type AnalyseSocketSendParams = StudySocketSendParams &
  EvalCacheSocketParams & { startWatching: (gameId: string) => void };

export type StudySocketSend = <K extends keyof StudySocketSendParams>(
  event: K,
  ...args: Parameters<StudySocketSendParams[K]>
) => void;
export type AnalyseSocketSend = <K extends keyof AnalyseSocketSendParams>(
  event: K,
  ...args: Parameters<AnalyseSocketSendParams[K]>
) => void;

export interface Socket {
  send: AnalyseSocketSend;
  receive(type: string, data: any): boolean;
  sendAnaMove(d: AnaMove): void;
  sendAnaDrop(d: AnaDrop): void;
}

export function make(send: AnalyseSocketSend, ctrl: AnalyseCtrl): Socket {
  // forecast mode: reload when opponent moves
  if (!ctrl.synthetic)
    setTimeout(function () {
      send('startWatching', ctrl.data.game.id);
    }, 1000);

  const handlers = {
    opening({ fen, opening }: { fen: FEN; opening: Opening }) {
      console.log(fen, opening);
      // ctrl.setOpening(fen, opening);
    },
    fen(e: GameUpdate) {
      if (
        ctrl.forecast &&
        e.id === ctrl.data.game.id &&
        treeOps.last(ctrl.mainline)!.fen.indexOf(e.fen) !== 0
      )
        ctrl.forecast.reloadToLastPly();
    },
    analysisProgress(data: ServerEvalData) {
      ctrl.mergeAnalysisData(data);
    },
    evalHit: ctrl.evalCache.onCloudEval,
  };

  function withoutStandardVariant(obj: { variant?: VariantKey }) {
    if (obj.variant === 'standard') delete obj.variant;
  }

  function sendAnaMove(req: AnaMove) {
    const studyData = ctrl.study?.socketSendNodeData();
    if (studyData) {
      withoutStandardVariant(req);
      send('anaMove', { ...req, ...studyData });
    }
  }

  function sendAnaDrop(req: AnaDrop) {
    const studyData = ctrl.study?.socketSendNodeData();
    if (studyData) {
      withoutStandardVariant(req);
      send('anaDrop', { ...req, ...studyData });
    }
  }

  return {
    receive(type, data) {
      const handler = (handlers as SocketHandlers)[type];
      if (handler) {
        handler(data);
        return true;
      }
      return !!ctrl.study?.socketHandler(type, data);
    },
    sendAnaMove,
    sendAnaDrop,
    send,
  };
}
