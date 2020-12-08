var m = require("mithril");
var makeItems = require("./item").ctrl;
var itemView = require("./item").view;
var makeScenario = require("./scenario");
var makeShogi = require("./chess");
var ground = require("./ground");
var scoring = require("./score");
var sound = require("./sound");
var promotion = require("./promotion");

module.exports = function (blueprint, opts) {
  var items = makeItems({
    apples: blueprint.apples,
  });

  var vm = {
    lastStep: false,
    completed: false,
    willComplete: false,
    failed: false,
    score: 0,
    nbMoves: 0,
  };

  var complete = function () {
    vm.willComplete = true;
    setTimeout(
      function () {
        vm.lastStep = false;
        vm.completed = true;
        sound.levelEnd();
        vm.score += scoring.getLevelBonus(blueprint, vm.nbMoves);
        ground.stop();
        m.redraw();
        if (!blueprint.nextButton) setTimeout(opts.onComplete, 1200);
      },
      ground.data().stats.dragged ? 1 : 250
    );
  };

  // cheat
  Mousetrap.bind(["shift+enter"], complete);

  var assertData = function () {
    return {
      scenario: scenario,
      shogi: shogi,
      vm: vm,
    };
  };

  var detectFailure = function () {
    var failed = blueprint.failure && blueprint.failure(assertData());
    if (failed) sound.failure();
    return failed;
  };

  var detectSuccess = function () {
    if (blueprint.success) return blueprint.success(assertData());
    else return !items.hasItem("apple");
  };

  var detectCapture = function () {
    if (!blueprint.detectCapture) return false;
    var fun =
      blueprint.detectCapture === "unprotected"
        ? "findUnprotectedCapture"
        : "findCapture";
    var move = shogi[fun]();
    if (!move) return;
    vm.failed = true;
    ground.stop();
    ground.showCapture(move);
    sound.failure();
    return true;
  };

  var sendMove = function (orig, dest, prom) {
    vm.nbMoves++;
    var move = shogi.move(orig, dest, prom);
    if (move) ground.fen(shogi.fen(), blueprint.color, {});
    else {
      // moving into check
      vm.failed = true;
      ground.showCheckmate(shogi);
      sound.failure();
      return m.redraw();
    }
    var took = false,
      inScenario,
      captured = false;
    items.withItem(move.to, function (item) {
      if (item === "apple") {
        vm.score += scoring.apple;
        items.remove(move.to);
        took = true;
      }
    });
    if (!took && move.captured && blueprint.pointsForCapture) {
      if (blueprint.showPieceValues)
        vm.score += scoring.pieceValue(move.captured);
      else vm.score += scoring.capture;
      took = true;
    }
    ground.check(shogi);
    if (scenario.player(move.from + move.to + (move.promotion || ""))) {
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
        setTimeout(function () {
          var rm = shogi.playRandomMove();
          ground.fen(shogi.fen(), blueprint.color, {}, [rm.orig, rm.dest]);
        }, 600);
    } else {
      ground.select(dest);
      if (!inScenario) {
        shogi.color(blueprint.color);
        ground.color(blueprint.color, makeShogiDests());
      }
    }
    m.redraw();
  };

  var makeShogiDests = function () {
    return shogi.dests({
      illegal: blueprint.offerIllegalMove,
    });
  };

  var onMove = function (orig, dest) {
    var piece = ground.get(dest);
    if (!piece || piece.color !== blueprint.color) return;
    if (!promotion.start(orig, dest, sendMove)) sendMove(orig, dest);
  };

  var shogi = makeShogi(
    blueprint.fen,
    blueprint.emptyApples ? [] : items.appleKeys()
  );

  var scenario = makeScenario(blueprint.scenario, {
    shogi: shogi,
    makeShogiDests: makeShogiDests,
  });

  promotion.reset();

  ground.set({
    shogi: shogi,
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
    events: blueprint.events,
    lastMoves: blueprint.lastMoves,
    notation: document.getElementsByClassName("notation-0")[0] ? 0 : 1,
  });

  return {
    blueprint: blueprint,
    items: items,
    vm: vm,
    scenario: scenario,
    start: function () {
      sound.levelStart();
      if (shogi.color() !== blueprint.color)
        setTimeout(scenario.opponent, 1000);
    },
    onComplete: opts.onComplete,
    complete: complete,
  };
};
