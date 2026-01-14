// file://./../../analyse/src/ctrl.ts

declare namespace Tree {
  export type NodeId = string;
  export type Path = string;

  interface ClientEvalBase extends EvalScore {
    bestmove?: Uci;
    ponder?: Uci;
    fen: FEN;
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
    best?: Uci | '(none)';
    fen: FEN;
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
    ply: Ply;
    uci?: Uci;
    fen: FEN;
    comments?: Comment[];
    gamebook?: Gamebook;
    dests?: Dests;
    drops?: Key[];
    check?: boolean;
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
    collapsed?: boolean;
  }

  export interface NodeIncomplete extends NodeBase {
    id?: NodeId;
    children?: Node[];
  }

  export interface Node extends NodeBase {
    id: NodeId;
    children: Node[];
  }

  export interface NodeCrazy {
    pockets: [CrazyPocket, CrazyPocket];
  }

  export type CrazyPocket = { [role in Exclude<Role, 'king'>]?: number };

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

  export interface Shape {
    orig: Key;
    dest?: Key;
  }
}
