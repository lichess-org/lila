import { RoundData } from 'round';
import { Libot } from './bots/interfaces';

export interface LocalPlayOpts {
  mode: 'vsBot' | 'botVsBot';
  pref: any;
  i18n: any;
  data: RoundData;
  setup?: GameSetup;
}

export interface GameSetup {
  white?: string;
  black?: string;
  fen?: string;
  time?: string;
}
