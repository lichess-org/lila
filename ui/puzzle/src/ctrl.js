var m = require('mithril');
var treeBuild = require('tree').build;
var treeOps = require('tree').ops;
var treePath = require('tree').path;
var cevalCtrl = require('ceval').ctrl;
var readDests = require('chess').readDests;
var decomposeUci = require('chess').decomposeUci;
var sanToRole = require('chess').sanToRole;
var opposite = require('chessground/util').opposite;
var keyboard = require('./keyboard');
var socketBuild = require('./socket');
var moveTestBuild = require('./moveTest');
var mergeSolution = require('./solution');
var makePromotion = require('./promotion');
var computeAutoShapes = require('./autoShape');
var throttle = require('common').throttle;
var xhr = require('./xhr');
var sound = require('./sound');

module.exports = function(opts, i18n) {

  var vm = {};
  var data, tree, ceval, moveTest;
  var ground = m.prop();

  var setPath = function(path) {
    vm.path = path;
    vm.nodeList = tree.getNodeList(path);
    vm.node = treeOps.last(vm.nodeList);
    vm.mainline = treeOps.mainlineNodeList(tree.root);
  };

  function withGround(f) {
    var g = ground();
    if (g) return f(g);
  }

  var initiate = function(fromData) {
    data = fromData;
    tree = treeBuild(treeOps.reconstruct(data.game.treeParts));
    var initialPath = treePath.fromNodeList(treeOps.mainlineNodeList(tree.root));
    // play | try | view
    vm.mode = 'play';
    vm.loading = false;
    vm.round = null;
    vm.voted = null;
    vm.justPlayed = null;
    vm.resultSent = false;
    vm.lastFeedback = 'init';
    vm.initialPath = initialPath;
    vm.initialNode = tree.nodeAtPath(initialPath);

    setPath(treePath.init(initialPath));
    setTimeout(function() {
      jump(initialPath);
      m.redraw();
    }, 500);

    // just to delay button display
    vm.canViewSolution = false;
    setTimeout(function() {
      vm.canViewSolution = true;
      m.redraw();
    }, 5000);

    moveTest = moveTestBuild(vm, data.puzzle);

    withGround(function(g) {
      g.setAutoShapes([]);
      g.setShapes([]);
      showGround(g);
    });
    m.redraw();

    instanciateCeval();

    history.replaceState(null, null, '/training/' + data.puzzle.id);
  };

  var makeCgOpts = function() {
    var node = vm.node;
    var color = node.ply % 2 === 0 ? 'white' : 'black';
    var dests = readDests(node.dests);
    var movable = (vm.mode === 'view' || color === data.puzzle.color) ? {
      color: (dests && Object.keys(dests).length > 0) ? color : null,
      dests: dests || {}
    } : {
      color: null,
      dests: {}
    };
    var config = {
      fen: node.fen,
      orientation: data.puzzle.color,
      turnColor: color,
      movable: movable,
      premovable: {
        enabled: false
      },
      check: !!node.check,
      lastMove: uciToLastMove(node.uci)
    };
    if (node.ply >= vm.initialNode.ply) {
      if (!dests && !node.check) {
        // premove while dests are loading from server
        // can't use when in check because it highlights the wrong king
        config.turnColor = opposite(color);
        config.movable.color = color;
        config.premovable.enabled = true;
      } else if (vm.mode !== 'view' && color !== data.puzzle.color) {
        config.movable.color = data.puzzle.color;
        config.premovable.enabled = true;
      }
    }
    vm.cgConfig = config;
    return config;
  };

  var showGround = function(g) {
    g.set(makeCgOpts());
    if (!vm.node.dests) getDests();
  };

  var userMove = function(orig, dest, capture) {
    vm.justPlayed = orig;
    sound[capture ? 'capture' : 'move']();
    if (!promotion.start(orig, dest, sendMove)) sendMove(orig, dest);
  };

  var sendMove = function(orig, dest, prom) {
    var move = {
      orig: orig,
      dest: dest,
      fen: vm.node.fen,
      path: vm.path
    };
    if (prom) move.promotion = prom;
    socket.sendAnaMove(move);
  };

  var getDests = throttle(800, false, function() {
    if (!vm.node.dests && treePath.contains(vm.path, vm.initialPath))
    socket.sendAnaDests({
      fen: vm.node.fen,
      path: vm.path
    });
  });

  var uciToLastMove = function(uci) {
    return uci && [uci.substr(0, 2), uci.substr(2, 2)]; // assuming standard chess
  };

  var addNode = function(node, path) {
    var newPath = tree.addNode(node, path);
    jump(newPath);
    reorderChildren(path);
    m.redraw();
    withGround(function(g) { g.playPremove(); });

    var progress = moveTest();
    if (progress) applyProgress(progress);
    m.redraw();
  };

  var reorderChildren = function(path, recursive) {
    var node = tree.nodeAtPath(path);
    node.children.sort(function(c1, c2) {
      if (c1.puzzle === 'fail') return 1;
      if (c1.puzzle === 'retry') return 1;
      if (c1.puzzle === 'good') return -1;
      return 0;
    });
    if (recursive) node.children.forEach(function(child) {
      reorderChildren(path + child.id, true);
    });
  };

  var revertUserMove = function() {
    setTimeout(function() {
      withGround(function(g) { g.cancelPremove(); });
      userJump(treePath.init(vm.path));
      m.redraw();
    }, 500);
  };

  var applyProgress = function(progress) {
    if (progress === 'fail') {
      vm.lastFeedback = 'fail';
      revertUserMove();
      if (vm.mode === 'play') {
        vm.canViewSolution = true;
        vm.mode = 'try';
        sendResult(false);
      }
    } else if (progress === 'retry') {
      vm.lastFeedback = 'retry';
      revertUserMove();
    } else if (progress === 'win') {
      if (vm.mode !== 'view') {
        if (vm.mode === 'play') sendResult(true);
        vm.lastFeedback = 'win';
        vm.mode = 'view';
        withGround(showGround); // to disable premoves
        startCeval();
      }
    } else if (progress && progress.orig) {
      vm.lastFeedback = 'good';
      setTimeout(function() {
        socket.sendAnaMove(progress);
      }, 500);
    }
  };

  var sendResult = function(win) {
    if (vm.resultSent) return;
    vm.resultSent = true;
    xhr.round(data.puzzle.id, win).then(function(res) {
      data.user = res.user;
      vm.round = res.round;
      vm.voted = res.voted;
      m.redraw();
    });
  };

  var nextPuzzle = function() {
    ceval.stop();
    vm.loading = true;
    m.redraw();
    xhr.nextPuzzle().done(function(d) {
      vm.round = null;
      vm.loading = false;
      initiate(d);
    });
  };

  var addDests = function(dests, path, opening) {
    tree.addDests(dests, path, opening);
    if (path === vm.path) {
      withGround(showGround);
      m.redraw();
      if (gameOver()) ceval.stop();
    }
    withGround(function(g) { g.playPremove(); });
  };

  var instanciateCeval = function(failsafe) {
    if (ceval) ceval.destroy();
    ceval = cevalCtrl({
      storageKeyPrefix: 'puzzle',
      multiPvDefault: 3,
      variant: {
        key: 'standard'
      },
      possible: true,
      emit: function(eval, work) {
        tree.updateAt(work.path, function(node) {
          if (work.threatMode) {
            if (!node.threat || node.threat.depth <= eval.depth || node.threat.maxDepth < eval.maxDepth)
            node.threat = eval;
          } else if (!node.ceval || node.ceval.depth <= eval.depth || node.ceval.maxDepth < eval.maxDepth)
          node.ceval = eval;
          if (work.path === vm.path) {
            setAutoShapes();
            m.redraw();
          }
        });
      },
      setAutoShapes: setAutoShapes,
      failsafe: failsafe,
      onCrash: function(e) {
        console.log('Local eval failed!', e);
        if (ceval.pnaclSupported) {
          console.log('Retrying in failsafe mode');
          instanciateCeval(true);
          startCeval();
        }
      }
    });
  };

  var setAutoShapes = function() {
    withGround(function(g) {
      g.setAutoShapes(computeAutoShapes({
        vm: vm,
        ceval: ceval,
        ground: g,
        nextNodeBest: nextNodeBest
      }));
    });
  };

  var canUseCeval = function() {
    return vm.mode === 'view' && !gameOver();
  }.bind(this);

  var startCeval = function() {
    if (ceval.enabled() && canUseCeval()) doStartCeval();
  }.bind(this);

  var doStartCeval = throttle(800, false, function() {
    ceval.start(vm.path, vm.nodeList, vm.threatMode);
  }.bind(this));

  var nextNodeBest = function() {
    return treeOps.withMainlineChild(vm.node, function(n) {
      return n.eval ? n.eval.pvs[0].moves[0] : null;
    });
  };

  var playUci = function(uci) {
    var move = decomposeUci(uci);
    if (!move[2]) sendMove(move[0], move[1])
    else sendMove(move[0], move[1], sanToRole[move[2].toUpperCase()]);
  };

  var getCeval = function() {
    return ceval;
  };

  var toggleCeval = function() {
    ceval.toggle();
    setAutoShapes();
    startCeval();
    if (!ceval.enabled()) vm.threatMode = false;
    vm.autoScrollRequested = true;
    m.redraw();
  };

  var toggleThreatMode = function() {
    if (vm.node.check) return;
    if (!ceval.enabled()) ceval.toggle();
    if (!ceval.enabled()) return;
    vm.threatMode = !vm.threatMode;
    setAutoShapes();
    startCeval();
    m.redraw();
  };

  var gameOver = function() {
    if (vm.node.dests !== '') return false;
    return vm.node.check ? 'checkmate' : 'stalemate';
  };

  var jump = function(path) {
    var pathChanged = path !== vm.path;
    setPath(path);
    withGround(showGround);
    if (pathChanged) {
      if (!vm.node.uci) sound.move(); // initial position
      else if (vm.node.uci.indexOf(vm.justPlayed) !== 0) {
        if (vm.node.san.indexOf('x') !== -1) sound.capture();
        else sound.move();
      }
      if (/\+|\#/.test(vm.node.san)) sound.check();
      vm.threatMode = false;
      ceval.stop();
      startCeval();
    }
    promotion.cancel();
    vm.justPlayed = null;
    vm.autoScrollRequested = true;
  };

  var userJump = function(path) {
    withGround(function(g) {
      g.selectSquare(null);
    });
    jump(path);
  };

  var viewSolution = function() {
    if (!vm.canViewSolution) return;
    sendResult(false);
    vm.mode = 'view';
    mergeSolution(tree, vm.initialNode, data.puzzle.branch, data.puzzle.color);
    reorderChildren(vm.initialPath, true);

    // try and play the solution next move
    var next = vm.node.children[0];
    if (next && next.puzzle === 'good') userJump(vm.path + next.id);
    else {
      var firstGoodPath = treeOps.takePathWhile(vm.mainline, function(node) {
        return node.puzzle !== 'good';
      });
      if (firstGoodPath) userJump(firstGoodPath + tree.nodeAtPath(firstGoodPath).children[0].id);
    }

    vm.autoScrollRequested = true;
    m.redraw();
    startCeval();
  };

  var socket = socketBuild({
    send: opts.socketSend,
    addNode: addNode,
    addDests: addDests,
    reset: function() {
      withGround(showGround);
      m.redraw();
    }
  });

  var recentHash = function() {
    return data.puzzle.id + (data.user ? data.user.recent.reduce(function(h, r) {
      return h + r[0];
    }, '') : '');
  };

  var hasEverVoted = lichess.storage.make('puzzle-ever-voted');

  var vote = throttle(1000, false, function(v) {
    hasEverVoted.set(1);
    vm.voted = v;
    xhr.vote(data.puzzle.id, v).then(function(res) {
      data.puzzle.vote = res[1];
      m.redraw();
    });
  });

  // required by ceval
  vm.showComputer = function() {
    return vm.mode === 'view';
  };
  vm.showAutoShapes = function() {
    return true;
  };

  initiate(opts.data);

  var promotion = makePromotion(vm, ground);

  keyboard.bind({
    vm: vm,
    userJump: userJump,
    getCeval: getCeval,
    toggleCeval: toggleCeval,
    toggleThreatMode: toggleThreatMode,
    playBestMove: function() {
      var uci = nextNodeBest() || (vm.node.ceval && vm.node.ceval.pvs[0].moves[0]);
      if (uci) playUci(uci);
    }
  });

  // If the page loads while being hidden (like when changing settings),
  // chessground is not displayed, and the first move is not fully applied.
  // Make sure chessground is fully shown when the page goes back to being visible.
  document.addEventListener('visibilitychange', function() {
    lichess.requestIdleCallback(function() {
      jump(vm.path);
    });
  });

  return {
    vm: vm,
    getData: function() {
      return data;
    },
    getTree: function() {
      return tree;
    },
    ground: ground,
    makeCgOpts: makeCgOpts,
    userJump: userJump,
    viewSolution: viewSolution,
    nextPuzzle: nextPuzzle,
    recentHash: recentHash,
    hasEverVoted: hasEverVoted,
    vote: vote,
    getCeval: getCeval,
    pref: opts.pref,
    trans: lichess.trans(opts.i18n),
    socketReceive: socket.receive,
    gameOver: gameOver,
    toggleCeval: toggleCeval,
    toggleThreatMode: toggleThreatMode,
    currentEvals: function() {
      return {
        client: vm.node.ceval
      };
    },
    nextNodeBest: nextNodeBest,
    userMove: userMove,
    playUci: playUci,
    showEvalGauge: function() {
      return vm.showComputer() && ceval.enabled();
    },
    getOrientation: function() {
      return withGround(function(g) { return g.state.orientation });
    },
    promotion: promotion
  };
}
