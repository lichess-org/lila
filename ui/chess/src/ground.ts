import { Api as CgApi } from 'chessground/api';

export type WithGround = <A>(f: (g: CgApi) => A) => A | undefined;
