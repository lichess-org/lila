import { build as treeBuild, ops as treeOps, path as treePath } from 'tree';
import { ctrl as cevalCtrl } from 'ceval';
import { readDests, decomposeUci, sanToRole } from 'chess';
import { opposite } from 'chessground/util';
import keyboard from './keyboard';
import socketBuild from './socket';
import moveTestBuild from './moveTest';
import mergeSolution from './solution';
import makePromotion from './promotion';
import computeAutoShapes from './autoShape';
import { prop } from 'common';
import { storedProp } from 'common/storage';
import throttle from 'common/throttle';
import * as xhr from './xhr';
import * as speech from './speech';
import { sound } from './sound';
import { Api as CgApi } from 'chessground/api';
import * as cg from 'chessground/types';
import { Vm, Controller } from './interfaces';

export default function(opts, redraw: () => void): Controller {

  let vm: Vm = {} as Vm;
  var data, tree, ceval, moveTest;
  const ground = prop<CgApi | undefined>(undefined);
  const threatMode = prop(false);

  // required by ceval
  vm.showComputer = () => vm.mode === 'view';
  vm.showAutoShapes = () => true;

  function setPath(path) {
    vm.path = path;
    vm.nodeList = tree.getNodeList(path);
    vm.node = treeOps.last(vm.nodeList)!;
    vm.mainline = treeOps.mainlineNodeList(tree.root);
  };

  function withGround<A>(f: (cg: CgApi) => A): A | undefined {
    const g = ground();
    if (g) return f(g);
  }

  function initiate(fromData) {
    data = fromData;
    tree = treeBuild(treeOps.reconstruct(data.game.treeParts));
    var initialPath = treePath.fromNodeList(treeOps.mainlineNodeList(tree.root));
    // play | try | view
    vm.mode = 'play';
    vm.loading = false;
    vm.round = undefined;
    vm.voted = undefined;
    vm.justPlayed = undefined;
    vm.resultSent = false;
    vm.lastFeedback = 'init';
    vm.initialPath = initialPath;
    vm.initialNode = tree.nodeAtPath(initialPath);

    setPath(treePath.init(initialPath));
    setTimeout(function() {
      jump(initialPath);
      redraw();
    }, 500);

    // just to delay button display
    vm.canViewSolution = false;
    setTimeout(function() {
      vm.canViewSolution = true;
      redraw();
    }, 5000);

    moveTest = moveTestBuild(vm, data.puzzle);

    withGround(function(g) {
      g.setAutoShapes([]);
      g.setShapes([]);
      showGround(g);
    });

    instanciateCeval();

    history.replaceState(null, '', '/training/' + data.puzzle.id);
  };

  var makeCgOpts = function() {
    const node = vm.node;
    const color: Color = node.ply % 2 === 0 ? 'white' : 'black';
    const dests = readDests(node.dests);
    const movable = (vm.mode === 'view' || color === data.puzzle.color) ? {
      color: (dests && Object.keys(dests).length > 0) ? color : null,
      dests: dests || {}
    } : {
      color: null,
      dests: {}
    };
    const config = {
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

  function showGround(g) {
    g.set(makeCgOpts());
    if (!vm.node.dests) getDests();
  };

  function userMove(orig, dest) {
    vm.justPlayed = orig;
    if (!promotion.start(orig, dest, sendMove)) sendMove(orig, dest);
  };

  function sendMove(orig: Key, dest: Key, prom?: cg.Role) {
    const move: any = {
      orig: orig,
      dest: dest,
      fen: vm.node.fen,
      path: vm.path
    };
    if (prom) move.promotion = prom;
    socket.sendAnaMove(move);
  };

  var getDests = throttle(800, function() {
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
    redraw();
    withGround(function(g) { g.playPremove(); });

    var progress = moveTest();
    if (progress) applyProgress(progress);
    redraw();
    speech.node(node, false);
  };

  function reorderChildren(path: Tree.Path, recursive?: boolean) {
    var node = tree.nodeAtPath(path);
    node.children.sort(function(c1, _) {
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
      redraw();
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

  function sendResult(win) {
    if (vm.resultSent) return;
    vm.resultSent = true;
    nbToVoteCall(Math.max(0, parseInt(nbToVoteCall()) - 1));
    xhr.round(data.puzzle.id, win).then(function(res) {
      data.user = res.user;
      vm.round = res.round;
      vm.voted = res.voted;
      redraw();
      if (win) speech.success();
    });
  };

  function nextPuzzle() {
    ceval.stop();
    vm.loading = true;
    redraw();
    xhr.nextPuzzle().done(function(d) {
      vm.round = null;
      vm.loading = false;
      initiate(d);
      redraw();
    });
  };

  function addDests(dests, path, opening) {
    tree.addDests(dests, path, opening);
    if (path === vm.path) {
      withGround(showGround);
      // redraw();
      if (gameOver()) ceval.stop();
    }
    withGround(function(g) { g.playPremove(); });
  };

  function instanciateCeval() {
    if (ceval) ceval.destroy();
    ceval = cevalCtrl({
      redraw,
      storageKeyPrefix: 'puzzle',
      multiPvDefault: 3,
      variant: {
        short: 'Std',
        name: 'Standard',
        key: 'standard'
      },
      possible: true,
      emit: function(ev, work) {
        tree.updateAt(work.path, function(node) {
          if (work.threatMode) {
            if (!node.threat || node.threat.depth <= ev.depth || node.threat.maxDepth < ev.maxDepth)
              node.threat = ev;
          } else if (!node.ceval || node.ceval.depth <= ev.depth || node.ceval.maxDepth < ev.maxDepth)
            node.ceval = ev;
          if (work.path === vm.path) {
            setAutoShapes();
            redraw();
          }
        });
      },
      setAutoShapes: setAutoShapes,
    });
  };

  function setAutoShapes() {
    withGround(function(g) {
      g.setAutoShapes(computeAutoShapes({
        vm: vm,
        ceval: ceval,
        ground: g,
        threatMode: threatMode(),
        nextNodeBest: nextNodeBest()
      }));
    });
  };

  function canUseCeval() {
    return vm.mode === 'view' && !gameOver();
  };

  function startCeval() {
    if (ceval.enabled() && canUseCeval()) doStartCeval();
  };

  const doStartCeval = throttle(800, function() {
    ceval.start(vm.path, vm.nodeList, threatMode());
  });

  function nextNodeBest() {
    return treeOps.withMainlineChild(vm.node, function(n) {
      // return n.eval ? n.eval.pvs[0].moves[0] : null;
      return n.eval ? n.eval.best : undefined;
    });
  };

  function playUci(uci) {
    var move = decomposeUci(uci);
    if (!move[2]) sendMove(move[0], move[1])
    else sendMove(move[0], move[1], sanToRole[move[2].toUpperCase()]);
  };

  function getCeval() {
    return ceval;
  };

  function toggleCeval() {
    ceval.toggle();
    setAutoShapes();
    startCeval();
    if (!ceval.enabled()) threatMode(false);
    vm.autoScrollRequested = true;
    redraw();
  };

  function toggleThreatMode() {
    if (vm.node.check) return;
    if (!ceval.enabled()) ceval.toggle();
    if (!ceval.enabled()) return;
    threatMode(!threatMode());
    setAutoShapes();
    startCeval();
    redraw();
  };

  function gameOver() {
    if (vm.node.dests !== '') return false;
    return vm.node.check ? 'checkmate' : 'draw';
  };

  function jump(path) {
    const pathChanged = path !== vm.path,
      isForwardStep = pathChanged && path.length === vm.path.length + 2;
    setPath(path);
    withGround(showGround);
    if (pathChanged) {
      if (isForwardStep) {
        if (!vm.node.uci) sound.move(); // initial position
        else if (!vm.justPlayed || vm.node.uci.includes(vm.justPlayed)) {
          if (vm.node.san!.includes('x')) sound.capture();
          else sound.move();
        }
        if (/\+|\#/.test(vm.node.san!)) sound.check();
      }
      threatMode(false);
      ceval.stop();
      startCeval();
    }
    promotion.cancel();
    vm.justPlayed = undefined;
    vm.autoScrollRequested = true;
    window.lichess.pubsub.emit('ply', vm.node.ply);
  };

  function userJump(path) {
    withGround(function(g) {
      g.selectSquare(null);
    });
    jump(path);
    speech.node(vm.node, true);
  };

  function viewSolution() {
    if (!vm.canViewSolution) return;
    sendResult(false);
    vm.mode = 'view';
    mergeSolution(vm.initialNode, data.puzzle.branch, data.puzzle.color);
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
    redraw();
    startCeval();
  };

  const socket = socketBuild({
    send: opts.socketSend,
    addNode: addNode,
    addDests: addDests,
    reset: function() {
      withGround(showGround);
      redraw();
    }
  });

  function recentHash(): string {
    return 'ph' + data.puzzle.id + (data.user ? data.user.recent.reduce(function(h, r) {
      return h + r[0];
    }, '') : '');
  }

  const nbToVoteCall = storedProp('puzzle.vote-call', 3);
  let thanksUntil: number | undefined;

  const callToVote = () => parseInt(nbToVoteCall()) < 1;

  const vote = throttle(1000, function(v) {
    if (callToVote()) thanksUntil = Date.now() + 2000;
    nbToVoteCall(5);
    vm.voted = v;
    xhr.vote(data.puzzle.id, v).then(function(res) {
      data.puzzle.vote = res[1];
      redraw();
    });
  });

  initiate(opts.data);

  const promotion = makePromotion(vm, ground, redraw);

  keyboard({
    vm,
    userJump,
    getCeval,
    toggleCeval,
    toggleThreatMode,
    redraw,
    playBestMove() {
      var uci = nextNodeBest() || (vm.node.ceval && vm.node.ceval.pvs[0].moves[0]);
      if (uci) playUci(uci);
    }
  });

  // If the page loads while being hidden (like when changing settings),
  // chessground is not displayed, and the first move is not fully applied.
  // Make sure chessground is fully shown when the page goes back to being visible.
  document.addEventListener('visibilitychange', function() {
    window.lichess.requestIdleCallback(function() {
      jump(vm.path);
    });
  });

  speech.setup();

  return {
    vm,
    getData() {
      return data;
    },
    getTree() {
      return tree;
    },
    ground,
    makeCgOpts,
    userJump,
    viewSolution,
    nextPuzzle,
    recentHash,
    callToVote,
    thanks() {
      return !!thanksUntil && Date.now() < thanksUntil;
    },
    vote,
    getCeval,
    pref: opts.pref,
    trans: window.lichess.trans(opts.i18n),
    socketReceive: socket.receive,
    gameOver,
    toggleCeval,
    toggleThreatMode,
    threatMode,
    currentEvals() {
      return { client: vm.node.ceval };
    },
    nextNodeBest,
    userMove,
    playUci,
    showEvalGauge() {
      return vm.showComputer() && ceval.enabled();
    },
    getOrientation() {
      return withGround(function(g) { return g.state.orientation })!;
    },
    getNode() {
      return vm.node;
    },
    showComputer: vm.showComputer,
    promotion,
    redraw,
    ongoing: false
  };
}
