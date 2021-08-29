export interface Vm {
  metric: Metric;
  dimension: Dimension;
  filters: Filters;
  loading: boolean;
  broken: boolean;
  answer: Chart | null;
  panel: 'filter' | 'preset';
}

export interface Env {
  i18n: { [key: string]: string };
  initialQuestion: Question;
  myUserId: string;
  pageUrl: string;
  postUrl: string;
  ui: UI;
  user: EnvUser;
}

export interface EnvUser {
  id: string;
  name: string;
  nbGames: number;
  shareId: number;
  stale: boolean;
}

export interface Question {
  metric: string;
  dimension: string;
  filters: Filters;
}

export type Filters = {
  [L in string]: string[];
};

export interface UI {
  dimensionCategs: Categ<Dimension>[];
  metricCategs: Categ<Metric>[];
  presets: Preset[];
}

export interface Categ<T> {
  name: string;
  items: T[];
}

interface Preset {
  name: string;
  dimension: string;
  metric: string;
  filters: Filters;
}

export interface Metric {
  key: string;
  name: string;
  position: string;
  description: string;
}

export interface Dimension extends Metric {
  values: {
    key: string;
    name: string;
  }[];
}

export interface Chart {
  question: Question;
  xAxis: Xaxis;
  valueYaxis: Yaxis;
  sizeYaxis: Yaxis;
  series: Serie[];
  sizeSerie: Serie;
  games: Game[];
}

interface Xaxis {
  name: string;
  categories: number[];
  dataType: string;
}

interface Yaxis {
  name: string;
  dataType: string;
}

interface Serie {
  name: string;
  dataType: string;
  stack?: string;
  data: number[];
}

export interface Game {
  id: string;
  fen: string;
  color: string;
  lastMove: string;
  user1: Player;
  user2: Player;
}

interface Player {
  name: string;
  title?: string;
  rating: number;
}
