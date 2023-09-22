import { Api as CgApi } from 'chessground/api';
import { Config as CgConfig } from 'chessground/config';
import * as draw from 'chessground/draw';
import * as types from 'chessground/types';

declare global {
  namespace cg {
    export type Api = CgApi;
    export type Config = CgConfig;
    export type Role = types.Role;
    export type Color = types.Color;
    export type Key = types.Key;
    export type Pos = types.Pos;
    export type Piece = types.Piece;
    export type Drop = types.Drop;
    export type DrawModifiers = draw.DrawModifiers;
    export type DrawShape = draw.DrawShape;
    export const pos2key: (pos: Pos) => Key;
    export const key2pos: (key: Key) => Pos;
    export const uciToMove: (uci: string | undefined) => Key[] | undefined;
    export const opposite: (color: Color) => Color;
  }
}
