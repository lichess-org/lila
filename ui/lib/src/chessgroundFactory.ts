type ChessgroundFactory = typeof import('@lichess-org/chessground').Chessground;
type DragNewPieceFactory = typeof import('@lichess-org/chessground/drag').dragNewPiece;

async function loadChessground(): Promise<[ChessgroundFactory, DragNewPieceFactory]> {
  if (localStorage.getItem('chessground3d') === 'true') {
    const [module, drag] = await Promise.all([import('chessground3D'), import('chessground3D/drag')]);
    return [module.Chessground, drag.dragNewPiece];
  }
  const [module, drag] = await Promise.all([
    import('@lichess-org/chessground'),
    import('@lichess-org/chessground/drag'),
  ]);
  return [module.Chessground, drag.dragNewPiece];
}

let chessgroundFactory: ChessgroundFactory | undefined;
let dragNewPieceFactory: DragNewPieceFactory | undefined;

export async function initChessgroundFactory(): Promise<void> {
  [chessgroundFactory, dragNewPieceFactory] = await loadChessground();
}

export const Chessground: ChessgroundFactory = (...args) => {
  if (!chessgroundFactory) {
    throw new Error('initChessgroundFactory() must be called before using Chessground');
  }
  return chessgroundFactory(...args);
};

export const dragNewPiece: DragNewPieceFactory = (...args) => {
  if (!dragNewPieceFactory) {
    throw new Error('initChessgroundFactory() must be called before using dragNewPiece');
  }
  return dragNewPieceFactory(...args);
};
