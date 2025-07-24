import { initial as initialBoardFEN } from '@lichess-org/chessground/fen';
import { ops as treeOps } from 'lib/tree/tree';
import type AnalyseCtrl from './ctrl';
import type { EvalGetData, EvalPutData, ServerEvalData } from './interfaces';
import type { AnaDests, AnaDrop, AnaMove, ChapterData, EditChapterData } from './study/interfaces';
import type { FormData as StudyFormData } from './study/studyForm';

interface DestsCache {
  [fen: string]: AnaDests;
}

interface AnaDestsReq {
  fen: FEN;
  path: string;
  ch?: string;
  variant?: VariantKey;
}

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

export type StudySocketSendParams =
  | [t: 'setPath', d: ReqPosition]
  | [t: 'deleteNode', d: ReqPosition & { jumpTo: string }]
  | [t: 'promote', d: ReqPosition & { toMainline: boolean }]
  | [t: 'forceVariation', d: ReqPosition & { force: boolean }]
  | [t: 'shapes', d: ReqPosition & { shapes: Tree.Shape[] }]
  | [t: 'setComment', d: ReqPosition & { text: string }]
  | [t: 'deleteComment', d: ReqPosition & { id: string }]
  | [t: 'setGamebook', d: ReqPosition & { gamebook: { deviation?: string; hint?: string } }]
  | [t: 'toggleGlyph', d: ReqPosition & { id: number }]
  | [t: 'explorerGame', d: ReqPosition & { gameId: string; insert: boolean }]
  | [t: 'setChapter', chapterId: string]
  | [t: 'setRole', d: { userId: string; role: string }]
  | [t: 'addChapter', d: ChapterData & { sticky?: boolean; showRatings?: boolean }]
  | [t: 'editChapter', d: EditChapterData]
  | [t: 'descStudy', desc: string]
  | [t: 'descChapter', d: { id: string; desc: string }]
  | [t: 'deleteChapter', chapterId: string]
  | [t: 'clearAnnotations', chapterId: string]
  | [t: 'clearVariations', chapterId: string]
  | [t: 'sortChapters', chapterIds: string[]]
  | [t: 'setTag', d: { chapterId: string; name: string; value: string }]
  | [t: 'anaMove', d: AnaMove & MoveOpts]
  | [t: 'anaDrop', d: AnaDrop & MoveOpts]
  | [t: 'anaDests', d: AnaDestsReq]
  | [t: 'like', d: { liked: boolean }]
  | [t: 'kick', username: string]
  | [t: 'editStudy', d: StudyFormData]
  | [t: 'setTopics', topics: string[]]
  | [t: 'requestAnalysis', chapterId: string]
  | [t: 'invite', username: string]
  | [t: 'relaySync', sync: boolean]
  | [t: 'leave'];

export type EvalCacheSocketParams = [t: 'evalPut', d: EvalPutData] | [t: 'evalGet', d: EvalGetData];

export type AnalyseSocketSendParams =
  | StudySocketSendParams
  | EvalCacheSocketParams
  | [t: 'startWatching', gameId: string];

export type StudySocketSend = (...[d, t]: StudySocketSendParams) => void;
export type AnalyseSocketSend = (...[d, t]: AnalyseSocketSendParams) => void;

export interface Socket {
  send: AnalyseSocketSend;
  receive(type: string, data: any): boolean;
  sendAnaMove(d: AnaMove): void;
  sendAnaDrop(d: AnaDrop): void;
  sendAnaDests(d: AnaDestsReq): void;
  clearCache(): void;
}

export function make(send: AnalyseSocketSend, ctrl: AnalyseCtrl): Socket {
  let anaMoveTimeout: number | undefined;
  let anaDestsTimeout: number | undefined;

  let anaDestsCache: DestsCache = {};

  function clearCache() {
    anaDestsCache =
      ctrl.data.game.variant.key === 'standard' && ctrl.tree.root.fen.split(' ', 1)[0] === initialBoardFEN
        ? {
            '': {
              path: '',
              dests: 'iqy muC gvx ltB bqs pxF jrz nvD ksA owE',
            },
          }
        : {};
  }
  clearCache();

  // forecast mode: reload when opponent moves
  if (!ctrl.synthetic)
    setTimeout(function () {
      send('startWatching', ctrl.data.game.id);
    }, 1000);

  function currentChapterId(): string | undefined {
    if (ctrl.study) return ctrl.study.vm.chapterId;
    return undefined;
  }

  function addStudyData(req: { ch?: string } & MoveOpts, isWrite = false) {
    const c = currentChapterId();
    if (c) {
      req.ch = c;
      if (isWrite) {
        if (ctrl.study!.isWriting()) {
          if (!ctrl.study!.vm.mode.sticky) req.sticky = false;
        } else req.write = false;
      }
    }
  }

  const handlers = {
    node(data: { ch?: string; node: Tree.Node; path: string }) {
      clearTimeout(anaMoveTimeout);
      if (data.ch === currentChapterId()) ctrl.addNode(data.node, data.path);
      else console.log('socket handler node got wrong chapter id', data);
    },
    stepFailure() {
      clearTimeout(anaMoveTimeout);
      ctrl.reset();
    },
    dests(data: AnaDests) {
      clearTimeout(anaDestsTimeout);
      if (!data.ch || data.ch === currentChapterId()) {
        anaDestsCache[data.path] = data;
        ctrl.addDests(data.dests, data.path);
      } else console.log('socket handler node got wrong chapter id', data);
    },
    destsFailure(data: any) {
      console.log(data);
      clearTimeout(anaDestsTimeout);
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

  function sendAnaDests(req: AnaDestsReq) {
    clearTimeout(anaDestsTimeout);
    if (anaDestsCache[req.path]) setTimeout(() => handlers.dests(anaDestsCache[req.path]), 300);
    else {
      withoutStandardVariant(req);
      addStudyData(req);
      send('anaDests', req);
      anaDestsTimeout = setTimeout(function () {
        console.log(req, 'resendAnaDests');
        sendAnaDests(req);
      }, 3000);
    }
  }

  function sendAnaMove(req: AnaMove) {
    clearTimeout(anaMoveTimeout);
    withoutStandardVariant(req);
    addStudyData(req, true);
    send('anaMove', req);
    anaMoveTimeout = setTimeout(() => sendAnaMove(req), 3000);
  }

  function sendAnaDrop(req: AnaDrop) {
    clearTimeout(anaMoveTimeout);
    withoutStandardVariant(req);
    addStudyData(req, true);
    send('anaDrop', req);
    anaMoveTimeout = setTimeout(() => sendAnaDrop(req), 3000);
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
    sendAnaDests,
    clearCache,
    send,
  };
}
