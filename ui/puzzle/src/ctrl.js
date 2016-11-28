var m = require('mithril');
var treeBuild = require('tree').build;
var treeOps = require('tree').ops;
var cevalCtrl = require('ceval').ctrl;
var readDests = require('chess').readDests;
var k = Mousetrap;
var chessground = require('chessground');
var opposite = chessground.util.opposite;
var groundBuild = require('./ground');
var socketBuild = require('./socket');
var throttle = require('common').throttle;
var xhr = require('./xhr');
var sound = require('./sound');

module.exports = function(opts, i18n) {

  var vm = {
    loading: false,
    justPlayed: null,
  };

  var data = opts.data;
  var tree = treeBuild(treeOps.reconstruct(opts.data.game.treeParts));
  var ground;

  var setPath = function(path) {
    vm.path = path;
    vm.nodeList = tree.getNodeList(path);
    vm.node = treeOps.last(vm.nodeList);
    vm.mainline = treeOps.mainlineNodeList(tree.root);
  };

  setPath('');

  var showGround = function() {
    var node = vm.node;
    var color = node.ply % 2 === 0 ? 'white' : 'black';
    var dests = readDests(node.dests);
    var config = {
      fen: node.fen,
      turnColor: color,
      movable: {
        color: (dests && Object.keys(dests).length > 0) ? color : null,
        dests: dests || {}
      },
      check: node.check,
      lastMove: uciToLastMove(node.uci)
    };
    if (!dests && !node.check) {
      // premove while dests are loading from server
      // can't use when in check because it highlights the wrong king
      config.turnColor = opposite(color);
      config.movable.color = color;
    }
    vm.cgConfig = config;
    if (!ground) ground = groundBuild(data, config, userMove);
    ground.set(config);
    if (!dests) getDests();
  };

  var userMove = function(orig, dest, capture) {
    vm.justPlayed = orig;
    sound[capture ? 'capture' : 'move']();
    sendMove(orig, dest);
  };

  var getDests = throttle(800, false, function() {
    if (!vm.node.dests) socket.sendAnaDests({
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
    m.redraw();
    ground.playPremove();
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

  var socket = socketBuild({
    send: opts.socketSend,
    addNode: addNode,
    addDests: addDests,
    reset: function() {
      showGround();
      m.redraw();
    }
  });

  showGround();

  console.log(data);

  return {
    vm: vm,
    tree: tree,
    ground: ground,
    userJump: userJump,
    trans: lichess.trans(opts.i18n),
    socketReceive: socket.receive
  };
}
