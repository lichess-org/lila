import type { Api as CgApi } from '@lichess-org/chessground/api';

export type WithGround = <A>(f: (g: CgApi) => A) => A | undefined;
