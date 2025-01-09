declare global {
  namespace Tree {
    type Path = string;

    interface EvalBase {
      sfen: Sfen;
      depth: number;
      nodes: number;
      pvs: PvData[];
      cp?: number;
      mate?: number;
    }
    interface CloudEval extends EvalBase {
      cloud: true;
      maxDepth: undefined;
      millis: undefined;
    }
    interface LocalEval extends EvalBase {
      cloud?: false;
      enteringKingRule: boolean;
      maxDepth: number;
      knps: number;
      millis: number;
    }
    type ClientEval = CloudEval | LocalEval;

    interface ServerEval {
      best?: Usi;
      sfen: Sfen;
      knodes: number;
      depth: number;
      pvs: PvDataServer[];
      cp?: number;
      mate?: number;
      path: string;
    }

    interface PvDataServer {
      mate?: number;
      cp?: number;
      moves: string;
    }

    interface PvData {
      moves: string[];
      mate?: number;
      cp?: number;
    }

    interface TablebaseHit {
      winner: Color | undefined;
      best?: Usi;
    }

    interface Node {
      id: string;
      ply: Ply;
      usi?: Usi;
      notation?: string;
      sfen: Sfen;
      children: Node[];
      comments?: Comment[];
      gamebook?: Gamebook;
      check?: boolean;
      capture?: boolean;
      threat?: LocalEval;
      ceval?: ClientEval;
      eval?: ServerEval;
      tbhit?: TablebaseHit | null;
      glyphs?: Glyph[];
      clock?: Clock;
      parentClock?: Clock;
      forceVariation?: boolean;
      shapes?: Shape[];
      comp?: boolean;
      fourfold?: boolean;
      fail?: boolean;
      puzzle?: 'win' | 'fail' | 'good';
    }

    interface Comment {
      id: string;
      by: CommentAuthor;
      text: string;
    }
    type CommentAuthor =
      | string
      | {
          id: string;
          name: string;
        }
      | null;

    interface Gamebook {
      deviation?: string;
      hint?: string;
      shapes?: Shape[];
    }

    type GlyphId = number;

    interface Glyph {
      id: GlyphId;
      name: string;
      symbol: string;
    }

    type Clock = number;

    interface Shape {}
  }
}

export {};
