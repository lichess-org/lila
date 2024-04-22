import { RoundData } from 'round';

export interface LocalPlayOpts {
  mode: 'vsBot' | 'botVsBot';
  pref: any;
  i18n: any;
  data: RoundData;
}
