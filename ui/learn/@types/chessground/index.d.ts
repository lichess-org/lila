interface Chessground {
  new (container: HTMLElement, options?: chessground.Options): chessground.Api;
  controller: {
    new (options?: chessground.Options): chessground.Ctrl;
  };
  view(ctrl: chessground.Ctrl): {
    tag: string;
    attrs: any;
    chilren: any[];
  };
  util: chessground.Util;
  fen: any;
  configure: any;
  anim: any;
  board: any;
  drag: any;
}

declare namespace chessground {
  interface Util {
    pos2key(pos: [number, number]): Key;
    key2pos(key: Key): [number, number];
    opposite(color: Color): Color;
    files: any;
    ranks: any;
    invRanks: any;
    allPos: any;
    allKeys: any;
    invKeys: any;
    invertKey: any;
    translate: any;
    containsX: any;
    distance: any;
    eventPosition: any;
    partialApply: any;
    partial: any;
    transformProp: any;
    isTrident: any;
    requestAnimationFrame: any;
    isRightButton: any;
    memo: any;
  }

  interface Common {
    /** reconfigure the instance */
    set(
      options: Exclude<
        Options,
        {
          viewOnly: boolean;
          minimalDom: boolean;
        }
      >
    ): void;
    /** change the view angle */
    toggleOrientation(): void;
    /** get the view angle */
    getOrientation(): Color;
    /** get pieces on the board */
    getPieces(): Partial<Record<Key, Piece>>;
    /** get the material difference between white and black */
    getMaterialDiff(): Record<Color, Record<Role, number>>;
    /** get the current FEN position */
    getFen(): string;
    /** add and/or remove arbitrary pieces on the board */
    setPieces(pieces: Pieces): void;
    /** sets the king of this color in check. if no color is provided, the current turn color is used */
    setCheck(color?: Color): void;
    /** play the current premove, if any */
    playPremove(): void;
    playPredrop(): void;
    /** cancel the current premove, if any */
    cancelPremove(): void;
    cancelPredrop(): void;
    /** cancel the current move being made */
    cancelMove(): void;
    /** cancels current move and prevent further ones */
    stop(): void;
    explode(keys: Key[]): void;
    setAutoShapes(shapes: Shapes): void;
    setShapes(shapes: Shapes): void;
    data: Data;
  }

  interface Ctrl extends Common {
    selectSquare(key: Key, force?: boolean): void;
    /** perform a move programmatically */
    apiMove(orig: Key, dest: Key): void;
    apiNewPiece(piece: Piece, key: Key): void;
    vm: {
      stage?: number;
      exploding: boolean;
    };
  }

  interface Api extends Common {
    /** perform a move programmatically */
    move(orig: Key, dest: Key): void;
    newPiece(piece: Piece, key: Key): void;
  }

  type Role = 'king' | 'queen' | 'bishop' | 'knight' | 'rook' | 'pawn';

  type Piece = {
    color: Color;
    role: Role;
    promoted?: boolean;
  };

  type Color = 'white' | 'black';

  type Drop = {
    role: Role;
    key: Key;
  };

  type Pieces = Partial<Record<Key, Piece | null>>;

  type Dests = Partial<Record<Key, Key[]>>;

  type Key = string;

  type BrushName = 'green' | 'red' | 'blue' | 'yellow' | 'paleBlue' | 'paleGreen' | 'paleRed' | 'paleGrey';

  type Circle = {
    brush?: BrushName;
    orig: Key;
  };

  type Arrow = {
    brush?: BrushName;
    orig: Key;
    dest: Key;
  };

  type Shapes = (Circle | Arrow)[];

  type Options = RecursivePartial<Data>;

  interface Data {
    fen: string;
    pieces: Pieces;
    /** board orientation. white | black */
    orientation: Color;
    /** turn to play. white | black */
    turnColor: Color;
    check: any;
    /** squares part of the last move ["c3", "c4"] | null */
    lastMove: [Key, Key] | null;
    /** square currently selected "a1" | null */
    selected: Key | null;
    /** include coords attributes */
    coordinates: boolean;
    /** function that rerenders the board */
    render?(): void;
    /** function that rerenders the board using requestAnimationFrame */
    renderRAF?(): void;
    /** DOM element of the board, required for drag piece centering */
    element?: HTMLElement;
    /** function that calculates the board bounds */
    bounds?: any;
    /** immediately complete the castle by moving the rook after king move */
    autoCastle: boolean;
    /** don't bind events: the user will never be able to move pieces around */
    viewOnly: boolean;
    /** because who needs a context menu on a chessboard */
    disableContextMenu: boolean;
    /** listens to chessground.resize on document.body to clear bounds cache */
    resizable: boolean;
    /** add a data-key attribute to piece elements */
    pieceKey: boolean;
    highlight: {
      /** add last-move class to squares */
      lastMove: boolean;
      /** add check class to squares */
      check: boolean;
      /** add drag-over class to square when dragging over it */
      dragOver: boolean;
    };
    animation: {
      /** enable piece animations, moving and fading */
      enabled: boolean;
      /** animation duration in milliseconds */
      duration: number;
      current:
        | {
            start?: number;
            duration?: number;
            anims: Partial<Record<Key, [[number, number], [number, number]]>>;
            fading: {
              pos: [number, number];
              opacity: number;
              role: Role;
              color: Color;
            }[];
          }
        | Record<string, never>;
    };
    movable: {
      /** all moves are valid - board editor */
      free: boolean;
      /** color that can move. white | black | both | null */
      color: Color | 'both' | null;
      dests: Dests;
      /** when a piece is dropped outside the board. "revert" | "trash" */
      dropOff: 'revert' | 'trash';
      /** last dropped [orig, dest], not to be animated */
      dropped?: [Key, Key] | [];
      /** whether to add the move-dest class on squares */
      showDests: boolean;
      events: {
        /** called after the move has been played */
        after(orig: Key, dest: Key, metadata: any): void;
        /** called after a new piece is dropped on the board */
        afterNewPiece(role: Role, pos: Key): void;
      };
      /** castle by moving the king to the rook */
      rookCastle: boolean;
    };
    premovable: {
      /** allow premoves for color that can not move */
      enabled: boolean;
      /** whether to add the premove-dest class on squares */
      showDests: boolean;
      /** whether to allow king castle premoves */
      castle: boolean;
      /** premove destinations for the current selection */
      dests: Key[];
      /** keys of the current saved premove ["e2" "e4"] | null */
      current: [Key, Key] | null;
      events: {
        /** called after the premove has been set */
        set(orig: Key, dest: Key): void;
        /** called after the premove has been unset */
        unset(): void;
      };
    };
    predroppable: {
      /** allow predrops for color that can not move */
      enabled: boolean;
      /** current saved predrop {role: 'knight', key: 'e4'} | {} */
      current: Drop | Record<string, never>;
      events: {
        /** called after the predrop has been set */
        set(orig: Key, dest: Key): void;
        /** called after the predrop has been unset */
        unset(): void;
      };
    };
    draggable: {
      /** allow moves & premoves to use drag'n drop */
      enabled: boolean;
      /** minimum distance to initiate a drag, in pixels */
      distance: boolean;
      /** lets chessground set distance to zero when user drags pieces */
      autoDistance: boolean;
      /** center the piece on cursor at drag start */
      centerPiece: boolean;
      /** show ghost of piece being dragged */
      showGhost: boolean;
      current:
        | {
            /** orig key of dragging piece */
            orig: Key;
            /** x, y of the piece at original position */
            rel: [number, number];
            /** relative current position */
            pos: [number, number];
            /** piece center decay */
            dec: [number, number];
            /** square being moused over */
            over: Key;
            /** current cached board bounds */
            bounds: any;
            /** whether the drag has started, as per the distance setting */
            started: boolean;
          }
        | Record<string, never>;
    };
    selectable: {
      /** disable to enforce dragging over click-click move */
      enabled: boolean;
    };
    stats: {
      /** was last piece dragged or clicked? needs default to false for touch */
      dragged: boolean;
    };
    events: {
      /** called after the situation changes on the board */
      change(): void;
      /** called after a piece has been moved. */
      move(orig: Key, dest: Key, capturedPiece: Piece | null): void;
      dropNewPiece(role: Role, pos: Key): void;
      /** DEPRECATED called when a piece has been captured */
      capture(key: Key, piece: Piece): void;
      /** called when a square is selected */
      select(key: Key): void;
    };
    /** items on the board { render: key -> vdom } */
    items: {
      render: {
        [key: string]: any;
      };
    };
    drawable: {
      /** allows SVG drawings */
      enabled: boolean;
      eraseOnClick: boolean;
      onChange(shapes: Shapes): void;
      /** user shapes */
      shapes: Shapes;
      /** computer shapes */
      autoShapes: Shapes;
      current:
        | {
            /** orig key of drawing */
            orig: Key;
            /** relative current position */
            pos: [number, number];
            /** square being moused over */
            dest: Key;
            /** current cached board bounds */
            bounds: any;
            /** brush name for shape */
            brush: BrushName;
          }
        | Record<string, never>;
      brushes: Record<
        BrushName,
        {
          key: string;
          color: string;
          opacity: number;
          lineWidth: number;
        }
      >;
      /** drawable SVG pieces, used for crazyhouse drop */
      pieces: {
        baseUrl: string;
      };
    };
  }
}
declare const chessground: Chessground;

export = chessground;

/** https://stackoverflow.com/questions/41980195/recursive-partialt-in-typescript#64060332 */
type RecursivePartial<T> = {
  [P in keyof T]?: T[P] extends (infer U)[] ? Value<U>[] : Value<T[P]>;
};
type AllowedPrimitives = boolean | string | number;
type Value<T> = T extends AllowedPrimitives ? T : RecursivePartial<T>;
