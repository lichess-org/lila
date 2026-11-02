type ChessgroundFactory = typeof import('@lichess-org/chessground').Chessground;

async function loadChessground(): Promise<ChessgroundFactory> {
  const module =
    localStorage.getItem('chessground3d') === 'true'
      ? await import('chessground3D')
      : await import('@lichess-org/chessground');
  return module.Chessground;
}

let chessgroundFactory: ChessgroundFactory | undefined;

export async function initChessgroundFactory(): Promise<void> {
  chessgroundFactory = await loadChessground();
}

export const Chessground: ChessgroundFactory = (...args) => {
  if (!chessgroundFactory) {
    throw new Error('initChessgroundFactory() must be called before using Chessground');
  }
  return chessgroundFactory(...args);
};
