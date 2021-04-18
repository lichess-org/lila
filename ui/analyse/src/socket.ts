import { initial as initialBoardFen } from 'chessground/fen';
import { ops as treeOps } from 'tree';
import AnalyseCtrl from './ctrl';
import { CachedEval, ServerEvalData } from './interfaces';

type DestsCache = {
  [fen: string]: AnaDests;
};
type AnaDests = {
  dests: string;
  path: string;
  ch?: string;
  opening?: {
    eco: string;
    name: string;
  };
};

// TODO: Split into request types
export interface Req {
  ch?: string; // TODO: needs to be defined in studies before sending
  sticky?: boolean;
  write?: boolean;
  path: string;
  variant?: VariantKey;
  fen?: string;
  shapes?: Tree.Shape[];
  jumpTo?: Tree.Path;
  toMainline?: boolean;
  force?: boolean;
}

export interface Socket {
  send: SocketSend;
  receive(type: string, data: any): boolean;
  sendAnaMove(req: Req): void;
  sendAnaDrop(req: Req): void;
  sendAnaDests(req: Req): void;
  sendForecasts(req: Req): void;
  clearCache(): void;
}

export function make(send: SocketSend, ctrl: AnalyseCtrl): Socket {
  let anaMoveTimeout: number | undefined;
  let anaDestsTimeout: number | undefined;

  let anaDestsCache: DestsCache = {};

  function clearCache() {
    anaDestsCache =
      ctrl.data.game.variant.key === 'standard' && ctrl.tree.root.fen.split(' ', 1)[0] === initialBoardFen
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

  function addStudyData(req: Req, isWrite = false): void {
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
    node(data) {
      clearTimeout(anaMoveTimeout);
      // no strict equality here!
      if (data.ch == currentChapterId()) ctrl.addNode(data.node, data.path);
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
      if (ctrl.forecast && e.id === ctrl.data.game.id && treeOps.last(ctrl.mainline)!.fen.indexOf(e.fen) !== 0) {
        ctrl.forecast.reloadToLastPly();
      }
    },
    analysisProgress(data: ServerEvalData) {
      ctrl.mergeAnalysisData(data);
    },
    evalHit(e: CachedEval) {
      ctrl.evalCache.onCloudEval(e);
    },
  };

  function withoutStandardVariant(obj: Req) {
    if (obj.variant === 'standard') delete obj.variant;
  }

  function sendAnaDests(req: Req) {
    clearTimeout(anaDestsTimeout);
    if (anaDestsCache[req.path])
      setTimeout(function () {
        handlers.dests(anaDestsCache[req.path]);
      }, 300);
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

  function sendAnaMove(req: Req) {
    clearTimeout(anaMoveTimeout);
    withoutStandardVariant(req);
    addStudyData(req, true);
    send('anaMove', req);
    anaMoveTimeout = setTimeout(() => sendAnaMove(req), 3000);
  }

  function sendAnaDrop(req: Req) {
    clearTimeout(anaMoveTimeout);
    withoutStandardVariant(req);
    addStudyData(req, true);
    send('anaDrop', req);
    anaMoveTimeout = setTimeout(() => sendAnaDrop(req), 3000);
  }

  return {
    receive(type: string, data: any): boolean {
      const handler = (handlers as Dictionary<(data: any) => void>)[type];
      if (handler) handler(data);
      return !!ctrl.study && ctrl.study.socketHandler(type, data);
    },
    sendAnaMove,
    sendAnaDrop,
    sendAnaDests,
    sendForecasts(req) {
      send('forecasts', req);
    },
    clearCache,
    send,
  };
}
