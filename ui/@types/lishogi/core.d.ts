import type * as shogiops from 'shogiops/types';

declare global {
  interface Cookie {
    name: string;
    value: string;
    maxAge: number;
  }

  type Timeout = ReturnType<typeof setTimeout>;

  interface LightUser {
    id: string;
    name: string;
    title?: string;
    patron?: boolean;
  }
  interface LightUserOnline extends LightUser {
    online?: boolean;
  }

  interface Navigator {
    deviceMemory: number;
  }

  type Speed =
    | 'ultraBullet'
    | 'bullet'
    | 'blitz'
    | 'rapid'
    | 'classical'
    | 'correspondence'
    | 'unlimited';

  type Perf =
    | 'ultraBullet'
    | 'bullet'
    | 'blitz'
    | 'rapid'
    | 'classical'
    | 'correspondence'
    | 'minishogi'
    | 'chushogi'
    | 'annanshogi'
    | 'kyotoshogi'
    | 'checkshogi';

  type Color = shogiops.Color;

  type Files = shogiops.FileName;
  type Ranks = shogiops.RankName;
  type Key = shogiops.SquareName;

  type MoveNotation = string;
  type Usi = string;
  type Sfen = string;
  type Ply = number;

  type VariantKey = shogiops.Rules;
  interface Variant {
    key: VariantKey;
    name: string;
  }

  interface Paginator<A> {
    currentPage: number;
    maxPerPage: number;
    currentPageResults: Array<A>;
    nbResults: number;
    previousPage?: number;
    nextPage?: number;
    nbPages: number;
  }
}

export {};
