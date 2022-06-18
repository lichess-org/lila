import { ops as treeOps } from 'tree';
import AnalyseCtrl from './ctrl';

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
  sendForecasts(req: Req): void;
}

export function make(send: SocketSend, ctrl: AnalyseCtrl): Socket {
  let anaUsiTimeout: number | undefined;

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
    sendForecasts(req) {
      send('forecasts', req);
    },
    send,
  };
}
