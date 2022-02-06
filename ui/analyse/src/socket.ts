import { initial as initialBoardSfen } from 'shogiground/sfen';
import { ops as treeOps } from 'tree';
import AnalyseCtrl from './ctrl';

type DestCache = {
  [sfen: string]: DestCacheEntry;
};
type DestCacheEntry = {
  path: string;
  dests: string;
};

interface Handlers {
  [key: string]: any; // #TODO
}

interface Req {
  [key: string]: any; // #TODO
}

export interface Socket {
  send: SocketSend;
  receive(type: string, data: any): boolean;
  sendAnaUsi(req: Req): void;
  sendAnaDests(req: Req): void;
  sendForecasts(req: Req): void;
  clearCache(): void;
}

export function make(send: SocketSend, ctrl: AnalyseCtrl): Socket {
  let anaUsiTimeout: number | undefined;
  let anaDestsTimeout: number | undefined;

  let anaDestsCache: DestCache = {};

  function clearCache() {
    anaDestsCache = {};
    ctrl.data.game.variant.key === 'standard' && ctrl.tree.root.sfen.split(' ', 1)[0] === initialBoardSfen
      ? {
          '': {
            path: '',
            dests: 'aj fonp uD gpo vE clm wF dmln sB qponmlr ir tC enmo yH zI AJ xG',
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

  function addStudyData(req, isWrite = false): void {
    var c = currentChapterId();
    if (c) {
      req.ch = c;
      if (isWrite) {
        if (ctrl.study!.isWriting()) {
          if (!ctrl.study!.vm.mode.sticky) req.sticky = false;
        } else req.write = false;
      }
    }
  }

  const handlers: Handlers = {
    node(data) {
      clearTimeout(anaUsiTimeout);
      // no strict equality here!
      if (data.ch == currentChapterId()) ctrl.addNode(data.node, data.path);
      else console.log('socket handler node got wrong chapter id', data);
    },
    stepFailure() {
      clearTimeout(anaUsiTimeout);
      ctrl.reset();
    },
    sfen(e) {
      if (ctrl.forecast && e.id === ctrl.data.game.id && treeOps.last(ctrl.mainline)!.sfen.indexOf(e.sfen) !== 0) {
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
    },
  };

  function withoutStandardVariant(obj) {
    if (obj.variant === 'standard') delete obj.variant;
  }

  function sendAnaDests(req) {
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

  function sendAnaUsi(req) {
    clearTimeout(anaUsiTimeout);
    withoutStandardVariant(req);
    addStudyData(req, true);
    send('anaUsi', req);
    anaUsiTimeout = setTimeout(() => sendAnaUsi(req), 3000);
  }

  return {
    receive(type: string, data: any): boolean {
      const handler = handlers[type];
      if (handler) handler(data);
      return !!ctrl.study && ctrl.study.socketHandler(type, data);
    },
    sendAnaUsi,
    sendAnaDests,
    sendForecasts(req) {
      send('forecasts', req);
    },
    clearCache,
    send,
  };
}
