const m = require('mithril');
const makeItems = require('./item').ctrl;
const itemView = require('./item').view;
const makeScenario = require('./scenario');
const makeChess = require('./chess');
const ground = require('./ground');
const scoring = require('./score');
const sound = require('./sound');
const promotion = require('./promotion');
const timeouts = require('./timeouts');

module.exports = function (blueprint, opts) {
  const items = makeItems({
    apples: blueprint.apples,
  });

  const vm = {
    lastStep: false,
    completed: false,
    willComplete: false,
    failed: false,
    score: 0,
    nbMoves: 0,
  };

  const complete = function () {
    vm.willComplete = true;
    vm.score += scoring.getLevelBonus(blueprint, vm.nbMoves);
    opts.onCompleteImmediate();
    timeouts.setTimeout(
      function () {
        vm.lastStep = false;
        vm.completed = true;
        sound.levelEnd();
        ground.stop();
        m.redraw();
        if (!blueprint.nextButton) timeouts.setTimeout(opts.onComplete, 1200);
      },
      ground.data().stats.dragged ? 1 : 250
    );
  };

  // cheat
  // Mousetrap.bind(['shift+enter'], complete);

  const assertData = function () {
    return {
      scenario: scenario,
      chess: chess,
      vm: vm,
    };
  };

  const detectFailure = function () {
    const failed = blueprint.failure && blueprint.failure(assertData());
    if (failed) sound.failure();
    return failed;
  };

  const detectSuccess = function () {
    if (blueprint.success) return blueprint.success(assertData());
    else return !items.hasItem('apple');
  };

  const detectCapture = function () {
    if (!blueprint.detectCapture) return false;
    const fun = blueprint.detectCapture === 'unprotected' ? 'findUnprotectedCapture' : 'findCapture';
    const move = chess[fun]();
    if (!move) return;
    vm.failed = true;
    ground.stop();
    ground.showCapture(move);
    sound.failure();
    return true;
  };

  const sendMove = function (orig, dest, prom) {
    vm.nbMoves++;
    const move = chess.move(orig, dest, prom);
    if (move) ground.fen(chess.fen(), blueprint.color, {});
    else {
      // moving into check
      vm.failed = true;
      ground.showCheckmate(chess);
      sound.failure();
      return m.redraw();
    }
    let took = false,
      inScenario,
      captured = false;
    items.withItem(move.to, function (item) {
      if (item === 'apple') {
        vm.score += scoring.apple;
        items.remove(move.to);
        took = true;
      }
    });
    if (!took && move.captured && blueprint.pointsForCapture) {
      if (blueprint.showPieceValues) vm.score += scoring.pieceValue(move.captured);
      else vm.score += scoring.capture;
      took = true;
    }
    ground.check(chess);
    if (scenario.player(move.from + move.to + (move.promotion || ''))) {
      vm.score += scoring.scenario;
      inScenario = true;
    } else {
      captured = detectCapture();
      vm.failed = vm.failed || captured || detectFailure();
    }
    if (!vm.failed && detectSuccess()) complete();
    if (vm.willComplete) return;
    if (took) sound.take();
    else if (inScenario) sound.take();
    else sound.move();
    if (vm.failed) {
      if (blueprint.showFailureFollowUp && !captured)
        timeouts.setTimeout(function () {
          const rm = chess.playRandomMove();
          ground.fen(chess.fen(), blueprint.color, {}, [rm.orig, rm.dest]);
        }, 600);
    } else {
      ground.select(dest);
      if (!inScenario) {
        chess.color(blueprint.color);
        ground.color(blueprint.color, makeChessDests());
      }
    }
    m.redraw();
  };

  var makeChessDests = function () {
    return chess.dests({
      illegal: blueprint.offerIllegalMove,
    });
  };

  const onMove = function (orig, dest) {
    const piece = ground.get(dest);
    if (!piece || piece.color !== blueprint.color) return;
    if (!promotion.start(orig, dest, sendMove)) sendMove(orig, dest);
  };

  var chess = makeChess(blueprint.fen, blueprint.emptyApples ? [] : items.appleKeys());

  var scenario = makeScenario(blueprint.scenario, {
    chess: chess,
    makeChessDests: makeChessDests,
  });

  promotion.reset();

  ground.set({
    chess: chess,
    offerIllegalMove: blueprint.offerIllegalMove,
    autoCastle: blueprint.autoCastle,
    orientation: blueprint.color,
    onMove: onMove,
    items: {
      render: function (pos, key) {
        return items.withItem(key, itemView);
      },
    },
    shapes: blueprint.shapes,
  });

  return {
    blueprint: blueprint,
    items: items,
    vm: vm,
    scenario: scenario,
    start: function () {
      sound.levelStart();
      if (chess.color() !== blueprint.color) timeouts.setTimeout(scenario.opponent, 1000);
    },
    onComplete: opts.onComplete,
  };
};
