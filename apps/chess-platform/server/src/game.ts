import { Chess } from "chess.js";

export class ChessGame {
  readonly chess = new Chess();

  get fen() { return this.chess.fen(); }
  get pgn() { return this.chess.pgn(); }

  move(from: string, to: string, promotion?: "q" | "r" | "b" | "n") {
    return this.chess.move({ from, to, ...(promotion ? { promotion } : {}) });
  }

  status() {
    if (this.chess.isCheckmate()) return "mate" as const;
    if (this.chess.isStalemate()) return "stalemate" as const;
    if (this.chess.isDraw()) return "draw" as const;
    return "started" as const;
  }
}
