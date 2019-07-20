import { initial as initialBoardFen } from 'draughtsground/fen';
import { ops as treeOps } from 'tree';
import AnalyseCtrl from './ctrl';
import * as draughtsUtil from 'draughts';

type DestCache = {
  [fen: string]: DestCacheEntry
}
type DestCacheEntry = {
  path: string,
  dests: string
}

interface Handlers {
  [key: string]: any; // #TODO
}

interface Req {
  [key: string]: any; // #TODO
}

export interface Socket {
  send: SocketSend;
  receive(type: string, data: any): boolean;
  sendAnaMove(req: Req, puzzle?: boolean): void;
  sendAnaDrop(req: Req): void;
  sendAnaDests(req: Req, puzzle?: boolean): void;
  sendForecasts(req: Req): void;
  clearCache(): void;
}

export function make(send: SocketSend, ctrl: AnalyseCtrl): Socket {

  let anaMoveTimeout;
  let anaDestsTimeout;

  let anaDestsCache: DestCache = {};

  function clearCache() {
    const fenSplit: String[] = ctrl.tree.root.fen.split(':');
    anaDestsCache = (
      ctrl.data.game.variant.key === 'standard' &&
        fenSplit.slice(0, 3).join(':') === 'W:' + initialBoardFen
    ) ? {
      '': {
        path: '',
        dests: 'HCD GBC ID FAB EzA'
      }
    } : {};
  }
  clearCache();

  // forecast mode: reload when opponent moves
  if (!ctrl.synthetic) setTimeout(function() {
    send("startWatching", ctrl.data.game.id);
  }, 1000);

  function currentChapterId(): string | undefined {
    if (ctrl.study) return ctrl.study.vm.chapterId;
  };

  function addStudyData(req, isWrite = false): void {
    var c = currentChapterId();
    if (c) {
      req.ch = c;
      if (isWrite) {
        if (ctrl.study!.isWriting()) {
          if (!ctrl.study!.vm.mode.sticky) req.sticky = false;
        }
        else req.write = false;
      }
    }
  };

  const handlers: Handlers = {
    node(data) {
      clearTimeout(anaMoveTimeout);
      // no strict equality here!
      if (data.ch == currentChapterId()) {
        const treeNode = data.node as Tree.Node;
        if (treeNode.dests !== undefined && treeNode.dests.length > 1 && treeNode.dests[0] === '#')
          treeNode.captLen = draughtsUtil.readCaptureLength(treeNode.dests);
        ctrl.addNode(data.node, data.path);
      } else 
        console.log('socket handler node got wrong chapter id', data);
    },
    stepFailure() {
      clearTimeout(anaMoveTimeout);
      ctrl.reset();
    },
    dests(data) {
      clearTimeout(anaDestsTimeout);
      if (!data.ch || data.ch === currentChapterId()) {
        anaDestsCache[data.path] = data;
        ctrl.addDests(data.dests, data.path, data.opening, data.alternatives, data.destsUci);
      } else
        console.log('socket handler node got wrong chapter id', data);
    },
    destsFailure(data) {
      console.log(data);
      clearTimeout(anaDestsTimeout);
    },
    fen(e) {
      if (ctrl.forecast &&
        e.id === ctrl.data.game.id &&
        treeOps.last(ctrl.mainline)!.fen.indexOf(e.fen) !== 0) {
        ctrl.forecast.reloadToLastPly();
      }
    },
    analysisProgress(data) {
      ctrl.mergeAnalysisData(data);
    },
    evalHit(e) {
      ctrl.evalCache.onCloudEval(e);
    },
    crowd(d) {
      ctrl.evalCache.upgradable(d.nb > 2);
    }
  };

  function withoutStandardVariant(obj) {
    if (obj.variant === 'standard') delete obj.variant;
  }

  function sendAnaDests(req, puzzle?: boolean) {
    clearTimeout(anaDestsTimeout);
    if (anaDestsCache[req.path]) setTimeout(function() {
      handlers.dests(anaDestsCache[req.path]);
    }, 300);
    else {
      withoutStandardVariant(req);
      addStudyData(req);
      if (puzzle) req.puzzle = true;
      send('anaDests', req);
      anaDestsTimeout = setTimeout(function() {
        console.log(req, 'resendAnaDests');
        sendAnaDests(req);
      }, 3500);
    }
  }

  function sendAnaMove(req, puzzle?: boolean) {
    clearTimeout(anaMoveTimeout);
    withoutStandardVariant(req);
    addStudyData(req, true);
    if (puzzle) req.puzzle = true;
    send('anaMove', req);
    anaMoveTimeout = setTimeout(() => sendAnaMove(req, puzzle), 3500);
  }

  function sendAnaDrop(req) {
    clearTimeout(anaMoveTimeout);
    withoutStandardVariant(req);
    addStudyData(req, true);
    send('anaDrop', req);
    anaMoveTimeout = setTimeout(() => sendAnaDrop(req), 3500);
  }

  return {
    receive(type: string, data: any): boolean {
      const handler = handlers[type];
      if (handler) handler(data);
      return !!ctrl.study && ctrl.study.socketHandler(type, data);
    },
    sendAnaMove,
    sendAnaDrop,
    sendAnaDests,
    sendForecasts(req) { send('forecasts', req); },
    clearCache,
    send
  };
}
