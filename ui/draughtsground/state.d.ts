import { AnimCurrent } from './anim';
import { DragCurrent } from './drag';
import { Drawable } from './draw';
import * as cg from './types';
export interface State {
    pieces: cg.Pieces;
    orientation: cg.Color;
    turnColor: cg.Color;
    lastMove?: cg.Key[];
    selected?: cg.Key;
    coordinates: boolean;
    viewOnly: boolean;
    disableContextMenu: boolean;
    resizable: boolean;
    addPieceZIndex: boolean;
    pieceKey: boolean;
    highlight: {
        lastMove: boolean;
        check: boolean;
    };
    animation: {
        enabled: boolean;
        duration: number;
        current?: AnimCurrent;
    };
    movable: {
        free: boolean;
        color?: cg.Color | 'both';
        dests?: cg.Dests;
        captLen?: number;
        showDests: boolean;
        events: {
            after?: (orig: cg.Key, dest: cg.Key, metadata: cg.MoveMetadata) => void;
            afterNewPiece?: (role: cg.Role, key: cg.Key, metadata: cg.MoveMetadata) => void;
        };
        rookCastle: boolean;
    };
    premovable: {
        enabled: boolean;
        showDests: boolean;
        dests?: cg.Key[];
        variant?: string;
        current?: cg.KeyPair;
        events: {
            set?: (orig: cg.Key, dest: cg.Key, metadata?: cg.SetPremoveMetadata) => void;
            unset?: () => void;
        };
    };
    predroppable: {
        enabled: boolean;
        current?: {
            role: cg.Role;
            key: cg.Key;
        };
        events: {
            set?: (role: cg.Role, key: cg.Key) => void;
            unset?: () => void;
        };
    };
    draggable: {
        enabled: boolean;
        distance: number;
        autoDistance: boolean;
        centerPiece: boolean;
        showGhost: boolean;
        deleteOnDropOff: boolean;
        current?: DragCurrent;
    };
    selectable: {
        enabled: boolean;
    };
    stats: {
        dragged: boolean;
        ctrlKey?: boolean;
    };
    events: {
        change?: () => void;
        move?: (orig: cg.Key, dest: cg.Key, capturedPiece?: cg.Piece) => void;
        dropNewPiece?: (piece: cg.Piece, key: cg.Key) => void;
        select?: (key: cg.Key) => void;
    };
    items?: (pos: cg.Pos, key: cg.Key) => any | undefined;
    drawable: Drawable;
    exploding?: cg.Exploding;
    dom: cg.Dom;
    hold: cg.Timer;
}
export declare function defaults(): Partial<State>;
