import { State } from './state';
import { DrawShape, DrawBrush } from './draw';
import * as cg from './types';
export interface Config {
    fen?: cg.FEN;
    orientation?: cg.Color;
    turnColor?: cg.Color;
    captureLength?: number;
    lastMove?: cg.Key[];
    selected?: cg.Key;
    coordinates?: boolean;
    autoCastle?: boolean;
    viewOnly?: boolean;
    disableContextMenu?: boolean;
    resizable?: boolean;
    addPieceZIndex?: boolean;
    highlight?: {
        lastMove?: boolean;
        check?: boolean;
    };
    animation?: {
        enabled?: boolean;
        duration?: number;
    };
    movable?: {
        free?: boolean;
        color?: cg.Color | 'both';
        dests?: {
            [key: string]: cg.Key[];
        };
        showDests?: boolean;
        events?: {
            after?: (orig: cg.Key, dest: cg.Key, metadata: cg.MoveMetadata) => void;
            afterNewPiece?: (role: cg.Role, key: cg.Key, metadata: cg.MoveMetadata) => void;
        };
        rookCastle?: boolean;
    };
    premovable?: {
        enabled?: boolean;
        showDests?: boolean;
        castle?: boolean;
        variant?: string;
        dests?: cg.Key[];
        events?: {
            set?: (orig: cg.Key, dest: cg.Key, metadata?: cg.SetPremoveMetadata) => void;
            unset?: () => void;
        };
    };
    predroppable?: {
        enabled?: boolean;
        events?: {
            set?: (role: cg.Role, key: cg.Key) => void;
            unset?: () => void;
        };
    };
    draggable?: {
        enabled?: boolean;
        distance?: number;
        autoDistance?: boolean;
        centerPiece?: boolean;
        showGhost?: boolean;
        deleteOnDropOff?: boolean;
    };
    selectable?: {
        enabled?: boolean;
    };
    events?: {
        change?: () => void;
        move?: (orig: cg.Key, dest: cg.Key, capturedPiece?: cg.Piece) => void;
        dropNewPiece?: (piece: cg.Piece, key: cg.Key) => void;
        select?: (key: cg.Key) => void;
    };
    items?: (pos: cg.Pos, key: cg.Key) => any | undefined;
    drawable?: {
        enabled?: boolean;
        visible?: boolean;
        eraseOnClick?: boolean;
        shapes?: DrawShape[];
        autoShapes?: DrawShape[];
        brushes?: DrawBrush[];
        pieces?: {
            baseUrl?: string;
        };
    };
}
export declare function configure(state: State, config: Config): void;
