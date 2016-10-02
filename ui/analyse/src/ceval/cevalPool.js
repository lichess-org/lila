var m = require('mithril');

function makeHelper(makeWorker, terminateWorker, poolOpts, makeProtocol, protocolOpts) {
  var worker, protocol, api;

  var boot = function () {
    worker = makeWorker(poolOpts);
    protocol = makeProtocol(api, protocolOpts);
    worker.addEventListener('message', function(e) {
      protocol.received(e.data);
    }, true);
  };

  var stop = function() {
    var stopped = m.deferred(false);
    setTimeout(function () {
      stopped.reject();
    }, 1000);
    return protocol.stop(stopped);
  };

  api = {
    send: function(text) {
      worker.postMessage(text);
    },
    start: function (work) {
      stop().then(function () {
        protocol.start(work);
      }, function () {
        terminateWorker(worker);
        boot();
        protocol.start(work);
      });
    },
    stop: stop
  };

  boot();

  return api;
}

function makeWebWorker(makeProtocol, poolOpts, protocolOpts) {
  return makeHelper(function () {
    return new Worker(poolOpts.asmjs);
  }, function (worker) {
    worker.terminate();
  }, poolOpts, makeProtocol, protocolOpts);
}

function makePNaClModule(makeProtocol, poolOpts, protocolOpts) {
  return makeHelper(function () {
    var worker = document.createElement('embed');
    worker.setAttribute('src', poolOpts.pnacl);
    worker.setAttribute('type', 'application/x-pnacl');
    worker.setAttribute('width', '0');
    worker.setAttribute('height', '0');
    document.body.appendChild(worker);
    return worker;
  }, function () {}, poolOpts, makeProtocol, protocolOpts);
}

module.exports = function(makeProtocol, poolOpts, protocolOpts) {
  var workers = [];
  var token = -1;

  var getWorker = function() {
    initWorkers();
    token = (token + 1) % workers.length;
    return workers[token];
  };

  var initWorkers = function() {
    if (workers.length) return;

    if (poolOpts.pnacl && navigator.mimeTypes['application/x-pnacl'])
      workers.push(makePNaClModule(makeProtocol, poolOpts, protocolOpts));
    else
      for (var i = 1; i <= 4; i++)
        workers.push(makeWebWorker(makeProtocol, poolOpts, protocolOpts));
  }

  var stopAll = function() {
    workers.forEach(function(i) {
      i.stop();
    });
  };

  return {
    start: function(work) {
      stopAll();
      getWorker().start(work);
    },
    stop: stopAll,
    warmup: initWorkers
  };
};
