export type San = string;

export interface ReplayData {
  pgn: San[];
}

export interface ReplayOpts {
  pgn: string;
  i18n: I18nDict;
}
