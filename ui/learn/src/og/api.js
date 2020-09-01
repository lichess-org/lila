var board = require("./board");

module.exports = function (controller) {
  return {
    set: controller.set,
    toggleOrientation: controller.toggleOrientation,
    getOrientation: controller.getOrientation,
    getPieces: function () {
      return controller.data.pieces;
    },
    getMaterialDiff: function () {
      return board.getMaterialDiff(controller.data);
    },
    getFen: controller.getFen,
    move: controller.apiMove,
    newPiece: controller.apiNewPiece,
    setPieces: controller.setPieces,
    setCheck: controller.setCheck,
    playPremove: controller.playPremove,
    playPredrop: controller.playPredrop,
    cancelPremove: controller.cancelPremove,
    cancelPredrop: controller.cancelPredrop,
    cancelMove: controller.cancelMove,
    stop: controller.stop,
    explode: controller.explode,
    setAutoShapes: controller.setAutoShapes,
    setShapes: controller.setShapes,
    data: controller.data, // directly exposes chessground state for more messing around
  };
};
