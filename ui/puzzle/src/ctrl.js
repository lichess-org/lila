var m = require('mithril');
var treeBuild = require('tree').build;
var treeOps = require('tree').ops;
var treePath = require('tree').path;
var cevalCtrl = require('ceval').ctrl;
var readDests = require('chess').readDests;
var k = Mousetrap;
var chessground = require('chessground');
var keyboard = require('./keyboard');
var opposite = chessground.util.opposite;
var groundBuild = require('./ground');
var socketBuild = require('./socket');
var moveTestBuild = require('./moveTest');
var mergeSolution = require('./solution');
var throttle = require('common').throttle;
var xhr = require('./xhr');
var sound = require('./sound');

module.exports = function(opts, i18n) {

  var vm = {};
  var data, tree, ground, ceval, moveTest;

  var setPath = function(path) {
    vm.path = path;
    vm.nodeList = tree.getNodeList(path);
    vm.node = treeOps.last(vm.nodeList);
    vm.mainline = treeOps.mainlineNodeList(tree.root);
  };

  var initiate = function(fromData) {
    data = fromData;
    console.log(data);
    tree = treeBuild(treeOps.reconstruct(data.game.treeParts));
    var initialPath = treePath.fromNodeList(treeOps.mainlineNodeList(tree.root));
    vm.mode = 'play'; // play | try | view
    vm.loading = false;
    vm.round = null;
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

    vm.canViewSolution = false; // just to delay button display
    setTimeout(function() {
      vm.canViewSolution = true;
      m.redraw();
    // }, 5000);
    }, 50);

    socket.setDestsCache(data.game.destsCache);
    moveTest = moveTestBuild(vm, data.puzzle);

    showGround();
    m.redraw();

    if (window.history.pushState)
      window.history.replaceState(null, null, '/training/' + data.puzzle.id);
  };

  var showGround = function() {
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
      check: node.check,
      lastMove: uciToLastMove(node.uci)
    };
    if (!dests && !node.check) {
      // premove while dests are loading from server
      // can't use when in check because it highlights the wrong king
      config.turnColor = opposite(color);
      config.movable.color = color;
      config.premovable.enabled = true;
    } else if (vm.mode !== 'view' && color !== data.puzzle.color) { //  && !node.check) {
      config.turnColor = color;
      config.movable.color = data.puzzle.color;
      config.premovable.enabled = true;
    }
    if (!ground) ground = groundBuild(data, config, userMove);
    ground.set(config);
    if (!dests) getDests();
  };

  var userMove = function(orig, dest, capture) {
    vm.justPlayed = orig;
    sound[capture ? 'capture' : 'move']();
    sendMove(orig, dest);
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

  // var preparePremoving = function() {
  //   ground.set({
  //     turnColor: ground.data.movable.color,
  //     movable: {
  //       color: opposite(ground.data.movable.color)
  //     }
  //   });
  // };

  var getDests = throttle(800, false, function() {
    if (!vm.node.dests && treePath.contains(vm.path, vm.initialPath))
      socket.sendAnaDests({
        fen: vm.node.fen,
        path: vm.path
      });
  });

  var uciToLastMove = function(uci) {
    if (!uci) return;
    return [uci.substr(0, 2), uci.substr(2, 2)]; // assuming standard chess
  };

  var addNode = function(node, path) {
    var newPath = tree.addNode(node, path);
    jump(newPath);
    reorderChildren(path);
    m.redraw();
    ground.playPremove();

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
      ground.cancelPremove();
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
      if (vm.mode === 'play') {
        vm.lastFeedback = 'win';
        vm.mode = 'view';
        showGround(); // to disable premoves
        sendResult(true);
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
    vm.loading = true;
    xhr.round(data.puzzle.id, win).then(function(res) {
      data.user = res.user;
      vm.round = res.round;
      vm.loading = false;
      m.redraw();
    });
  };

  var nextPuzzle = function() {
    vm.loading = true;
    xhr.nextPuzzle().then(function(d) {
      // pushState(cfg);
      vm.round = null;
      vm.loading = false;
      initiate(d);
    });
  };

  var addDests = function(dests, path, opening) {
    tree.addDests(dests, path, opening);
    if (path === vm.path) {
      showGround();
      m.redraw();
      if (gameOver()) ceval.stop();
    }
    ground.playPremove();
  };

  var instanciateCeval = function(failsafe) {
    ceval = cevalCtrl({
      variant: data.game.variant,
      possible: true,
      emit: function(res) {
        tree.updateAt(res.work.path, function(node) {
          if (node.ceval && node.ceval.depth >= res.eval.depth) return;
          node.ceval = res.eval;
          // if (res.work.path === vm.path) {
          //   setAutoShapes();
          //   m.redraw();
          // }
        });
      },
      setAutoShapes: $.noop,
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
  // instanciateCeval();

  var gameOver = function() {
    if (vm.node.dests !== '') return false;
    if (vm.node.check) {
      var san = vm.node.san;
      var checkmate = san && san[san.length - 1] === '#';
      return checkmate;
    }
    return true;
  };

  var jump = function(path) {
    var pathChanged = path !== vm.path;
    setPath(path);
    showGround();
    if (pathChanged) {
      if (!vm.node.uci) sound.move(); // initial position
      else if (vm.node.uci.indexOf(vm.justPlayed) !== 0) {
        if (vm.node.san.indexOf('x') !== -1) sound.capture();
        else sound.move();
      }
      if (/\+|\#/.test(vm.node.san)) sound.check();
      // this.vm.threatMode = false;
      // this.ceval.stop();
      // this.startCeval();
    }
    vm.justPlayed = null;
    vm.autoScrollRequested = true;
  };

  var userJump = function(path) {
    ground.selectSquare(null);
    jump(path);
  };

  var viewSolution = function() {
    sendResult(false);
    vm.mode = 'view';
    mergeSolution(tree, vm.initialNode, data.puzzle.branch, data.puzzle.color);
    reorderChildren(vm.initialPath, true);
    vm.autoScrollRequested = true;
    m.redraw();
  };

  var socket = socketBuild({
    send: opts.socketSend,
    addNode: addNode,
    addDests: addDests,
    reset: function() {
      showGround();
      m.redraw();
    }
  });

  var recentHash = function() {
    return data.user ? data.user.recent.reduce(function(h, r) {
      return h + r[0];
    }, '') : '';
  };

  initiate(opts.data);

  keyboard.bind({
    vm: vm,
    userJump: userJump
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
    userJump: userJump,
    viewSolution: viewSolution,
    nextPuzzle: nextPuzzle,
    recentHash: recentHash,
    trans: lichess.trans(opts.i18n),
    socketReceive: socket.receive
  };
}
