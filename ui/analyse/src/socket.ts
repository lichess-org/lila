import { synthetic } from './util';
import { initial as initialBoardFen } from 'chessground/fen';
import { AnalyseController } from './interfaces';

type DestCache = {
  [fen: string]: DestCacheEntry
}
type DestCacheEntry = {
  path: string,
  dests: string
}

export default function(send, ctrl: AnalyseController) {

  let anaMoveTimeout;
  let anaDestsTimeout;

  let anaDestsCache: DestCache = {};

  function clearCache() {
    anaDestsCache = (
      ctrl.data.game.variant.key === 'standard' &&
        ctrl.tree.root.fen.split(' ', 1)[0] === initialBoardFen
    ) ? {
      '': {
        path: '',
        dests: 'iqy muC gvx ltB bqs pxF jrz nvD ksA owE'
      }
    } : {};
  };
  clearCache();

  // forecast mode: reload when opponent moves
  if (!synthetic(ctrl.data)) setTimeout(function() {
    send("startWatching", ctrl.data.game.id);
  }, 1000);

  function currentChapterId() {
    if (ctrl.study) return ctrl.study.vm.chapterId;
  };

  function addStudyData(req, isWrite = false) {
    var c = currentChapterId();
    if (c) {
      req.ch = c;
      if (isWrite) {
        if (ctrl.study!.vm.mode.write) {
          if (!ctrl.study!.vm.mode.sticky) req.sticky = false;
        }
        else req.write = false;
      }
    }
  };

  const handlers = {
    node(data) {
      clearTimeout(anaMoveTimeout);
      // no strict equality here!
      if (data.ch == currentChapterId())
        ctrl.addNode(data.node, data.path);
      else
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
        ctrl.addDests(data.dests, data.path, data.opening);
      } else
      console.log('socket handler node got wrong chapter id', data);
    },
    destsFailure(data) {
      console.log(data);
      clearTimeout(anaDestsTimeout);
    },
    fen(e) {
      if (ctrl.forecast && e.id === ctrl.data.game.id)
        ctrl.forecast.reloadToLastPly();
    },
    analysisProgress(data) {
      ctrl.mergeAnalysisData(data);
    },
    evalHit(e) {
      ctrl.evalCache.onCloudEval(e);
    }
  };

  function withoutStandardVariant(obj) {
    if (obj.variant === 'standard') delete obj.variant;
  }

  function sendAnaDests(req) {
    clearTimeout(anaDestsTimeout);
    if (anaDestsCache[req.path]) setTimeout(function() {
      handlers.dests(anaDestsCache[req.path]);
    }, 300);
    else {
      withoutStandardVariant(req);
      addStudyData(req);
      send('anaDests', req);
      anaDestsTimeout = setTimeout(function() {
        console.log(req, 'resendAnaDests');
        sendAnaDests(req);
      }, 3000);
    }
  }

  function sendAnaMove(req) {
    clearTimeout(anaMoveTimeout);
    withoutStandardVariant(req);
    addStudyData(req, true);
    send('anaMove', req);
    anaMoveTimeout = setTimeout(() => sendAnaMove(req), 3000);
  }

  function sendAnaDrop(req) {
    clearTimeout(anaMoveTimeout);
    withoutStandardVariant(req);
    addStudyData(req, true);
    send('anaDrop', req);
    anaMoveTimeout = setTimeout(() => sendAnaDrop(req), 3000);
  }

  return {
    receive(type, data) {
      if (handlers[type]) {
        handlers[type](data);
        return true;
      } else if (ctrl.study && ctrl.study.socketHandlers[type]) {
        ctrl.study.socketHandlers[type](data);
        return true;
      }
      return false;
    },
    sendAnaMove,
    sendAnaDrop,
    sendAnaDests,
    sendForecasts(req) {
      send('forecasts', req);
    }
  };
}
