var m = require('mithril');

function makeHelper(makeWorker, terminateWorker, poolOpts, makeProtocol, protocolOpts) {
  var worker, protocol, api;

  var boot = function() {
    worker = makeWorker(poolOpts);
    protocol = makeProtocol(api, protocolOpts);
    worker.addEventListener('message', function(e) {
      protocol.received(e.data);
    }, true);
  };

  var stop = function() {
    var stopped = protocol.stop();
    setTimeout(function() {
      stopped.reject();
    }, 1000);
    return stopped.promise;
  };

  api = {
    send: function(text) {
      worker.postMessage(text);
    },
    start: function(work) {
      stop().catch(function() {
        terminateWorker(worker);
        boot();
        return protocol.stop().promise;
      }).then(function() {
        protocol.start(work);
      });
    },
    stop: stop,
    destroy: function() {
      terminateWorker(worker);
    }
  };

  boot();

  return api;
}

function makeWebWorker(makeProtocol, poolOpts, protocolOpts) {
  return makeHelper(function() {
    return new Worker(poolOpts.wasm || poolOpts.asmjs);
  }, function(worker) {
    worker.terminate();
  }, poolOpts, makeProtocol, protocolOpts);
}

function makePNaClModule(makeProtocol, poolOpts, protocolOpts) {
  try {
    return makeHelper(function() {
      var worker = document.createElement('embed');
      worker.setAttribute('src', poolOpts.pnacl);
      worker.setAttribute('type', 'application/x-pnacl');
      worker.setAttribute('width', '0');
      worker.setAttribute('height', '0');
      document.body.appendChild(worker);
      ['crash', 'error'].forEach(function(eventType) {
        worker.addEventListener(eventType, function() {
          poolOpts.onCrash({
            lastError: worker.lastError,
            hash: protocolOpts.hashSize(),
            threads: protocolOpts.threads()
          });
        }, true);
      });
      return worker;
    }, function(worker) {
      worker.remove();
    }, poolOpts, makeProtocol, protocolOpts);
  } catch (e) {
    poolOpts.onCrash(e);
    return makeWorkerStub();
  }
}

function makeWorkerStub() {
  var noop = function() {};
  return {
    send: noop,
    start: noop,
    stop: noop,
    destroy: noop
  };
}

module.exports = function(makeProtocol, poolOpts, protocolOpts) {
  var workers = [];
  var token = 0;

  var getWorker = function() {
    initWorkers();

    // briefly wait and give a chance to reuse the current worker
    var worker = m.deferred();
    workers[token].stop().then(function() {
      worker.resolve(workers[token]);
    });
    setTimeout(function () {
      worker.reject();
    }, 50);

    return worker.promise.catch(function () {
      token = (token + 1) % workers.length;
      var w = m.deferred();
      w.resolve(workers[token]);
      return w.promise;
    });
  };

  var initWorkers = function() {
    if (workers.length) return;

    if (poolOpts.pnacl)
      workers.push(makePNaClModule(makeProtocol, poolOpts, protocolOpts));
    else
      for (var i = 1; i <= 2; i++)
        workers.push(makeWebWorker(makeProtocol, poolOpts, protocolOpts));
  }

  var stopAll = function() {
    workers.forEach(function(w) {
      w.stop();
    });
  };

  return {
    start: function(work) {
      lichess.storage.set('ceval.pool.start', 1);
      getWorker().then(function(worker) {
        worker.start(work);
      });
    },
    stop: stopAll,
    warmup: initWorkers,
    destroy: function() {
      workers.forEach(function(w) {
        w.stop();
        w.destroy();
      });
    }
  };
};
