$(function() {
  var board = new ChessBoard('board', {
    draggable: true,
    dropOffBoard: 'trash',
    position: 'start',
    sparePieces: true
  });
});
