var board = require("./board");
var data = require("./data");
var fen = require("./fen");
var configure = require("./configure");
var anim = require("./anim");
var drag = require("./drag");

module.exports = function (cfg) {
  this.data = data(cfg);

  this.vm = {
    exploding: false,
  };

  this.getFen = function () {
    return fen.write(this.data.pieces);
  }.bind(this);

  this.getOrientation = function () {
    return this.data.orientation;
  }.bind(this);

  this.set = anim(configure, this.data);

  this.toggleOrientation = function () {
    anim(board.toggleOrientation, this.data)();
    if (this.data.redrawCoords) this.data.redrawCoords(this.data.orientation);
  }.bind(this);

  this.setPieces = anim(board.setPieces, this.data);

  this.selectSquare = anim(board.selectSquare, this.data, true);

  this.apiMove = anim(board.apiMove, this.data);

  this.apiNewPiece = anim(board.apiNewPiece, this.data);

  this.playPremove = anim(board.playPremove, this.data);

  this.playPredrop = anim(board.playPredrop, this.data);

  this.cancelPremove = anim(board.unsetPremove, this.data, true);

  this.cancelPredrop = anim(board.unsetPredrop, this.data, true);

  this.setCheck = anim(board.setCheck, this.data, true);

  this.cancelMove = anim(
    function (data) {
      board.cancelMove(data);
      drag.cancel(data);
    }.bind(this),
    this.data,
    true
  );

  this.stop = anim(
    function (data) {
      board.stop(data);
      drag.cancel(data);
    }.bind(this),
    this.data,
    true
  );

  this.explode = function (keys) {
    if (!this.data.render) return;
    this.vm.exploding = {
      stage: 1,
      keys: keys,
    };
    this.data.renderRAF();
    setTimeout(
      function () {
        this.vm.exploding.stage = 2;
        this.data.renderRAF();
        setTimeout(
          function () {
            this.vm.exploding = false;
            this.data.renderRAF();
          }.bind(this),
          120
        );
      }.bind(this),
      120
    );
  }.bind(this);

  this.setAutoShapes = function (shapes) {
    anim(
      function (data) {
        data.drawable.autoShapes = shapes;
      },
      this.data,
      false
    )();
  }.bind(this);

  this.setShapes = function (shapes) {
    anim(
      function (data) {
        data.drawable.shapes = shapes;
      },
      this.data,
      false
    )();
  }.bind(this);
};
