import { Prop, StoredProp, StoredJsonProp } from 'common';

export interface Hovering {
  fen: Fen;
  uci: Uci;
}

export type ExplorerDb = 'lichess' | 'masters' | 'watkins';

export type ExplorerSpeed = 'bullet' | 'blitz' | 'classical';

export interface ExplorerConfigData {
  open: Prop<boolean>;
  db: {
    available: ExplorerDb[];
    selected: StoredProp<ExplorerDb>;
  };
  rating: {
    available: number[];
    selected: StoredJsonProp<number[]>;
  };
  speed: {
    available: ExplorerSpeed[];
    selected: StoredJsonProp<ExplorerSpeed[]>;
  };
}

export interface ExplorerConfigCtrl {
  trans: Trans;
  redraw();
  data: ExplorerConfigData;
  toggleOpen();
  toggleDb(db: ExplorerDb);
  toggleRating(rating: number);
  toggleSpeed(speed: string);
  fullHouse(): boolean;
}

export interface ExplorerData {
  fen: Fen;
  moves: any;
  // TODO
}

export interface ExplorerCtrl {
  allowed: Prop<boolean>;
  loading: Prop<boolean>;
  enabled: Prop<boolean>;
  failing: Prop<boolean>;
  movesAway: Prop<number>;
  config: ExplorerConfigCtrl;
  withGames: boolean;
  current(): ExplorerData
  hovering: Prop<Hovering | null>;
  setNode();
  toggle();
  disable();
  setHovering(fen: Fen, uci: Uci | null);
  fetchMasterOpening(fen: Fen): JQueryPromise<ExplorerData>
}
