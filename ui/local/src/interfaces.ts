import { RoundData } from 'round';
import { Libot } from './bots/interfaces';

export interface LocalPlayOpts {
  pref: any;
  i18n: any;
  data: RoundData;
  setup?: LocalSetup;
  testUi?: boolean;
}

export interface LocalSetup {
  white?: string;
  black?: string;
  fen?: string;
  nfold?: number;
  time?: string;
}

export interface GameObserver {
  onGameEnd: (result: 'white' | 'black' | 'draw', reason: string) => void;
}
