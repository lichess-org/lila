// file://./../../analyse/src/ctrl.ts

declare namespace Tree {
  export type Path = string;

  interface ClientEvalBase extends EvalScore {
    fen: CgFEN;
    depth: number;
    nodes: number;
    pvs: PvData[];
  }
  export interface CloudEval extends ClientEvalBase {
    cloud: true;
    millis?: undefined;
  }
  export interface LocalEval extends ClientEvalBase {
    cloud?: false;
    millis: number;
  }
  export type ClientEval = CloudEval | LocalEval;

  export interface ServerEval extends EvalScore {
    best?: Uci;
    fen: CgFEN;
    knodes: number;
    depth: number;
    pvs: PvDataServer[];
  }

  export interface PvDataServer extends EvalScore {
    moves: string;
  }

  export interface PvData extends EvalScore {
    moves: string[];
  }

  export interface TablebaseHit {
    winner: Color | undefined;
    best?: Uci;
  }

  export interface NodeBase {
    // file://./../../tree/src/tree.ts
    id: string;
    ply: Ply;
    uci?: Uci;
    fen: CgFEN;
    comments?: Comment[];
    gamebook?: Gamebook;
    dests?: string;
    drops?: string | null;
    check?: Key;
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
    san?: string;
    threefold?: boolean;
    fail?: boolean;
    puzzle?: 'win' | 'fail' | 'good' | 'retry';
    crazy?: NodeCrazy;
  }
  export interface NodeFromServer extends NodeBase {
    children?: Node[];
  }
  export interface Node extends NodeBase {
    children: Node[];
    collapsed?: boolean;
  }

  export interface NodeCrazy {
    pockets: [CrazyPocket, CrazyPocket];
  }

  export interface CrazyPocket {
    [role: string]: number;
  }

  export interface Comment {
    id: string;
    by:
      | string
      | {
          id: string;
          name: string;
        };
    text: string;
  }

  export interface Gamebook {
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

  export type Clock = number;

  export interface Shape {}
}
