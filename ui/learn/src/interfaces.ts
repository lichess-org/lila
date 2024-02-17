import { DrawShape, SquareHighlight } from 'shogiground/draw';
import { VNode } from 'snabbdom';

export type Score = 0 | 1 | 2 | 3;

export type StageKey = string;
export type StageId = number;
export type LevelId = number;
export type CategoryKey = I18nKey;

export interface UsiWithColor {
  usi: Usi;
  color: Color;
}

export type Shape = DrawShape | SquareHighlight;

export interface VmEvaluation<T> {
  (level: Level, usiCList: UsiWithColor[]): T;
}

export type Assertion = VmEvaluation<boolean>;

export type Scenario = UsiWithColor[];

export interface Level {
  id: LevelId;
  goal: I18nKey | ((trans: Trans) => VNode); // i18n string displayed on the right
  sfen: Sfen; // starting position of the level
  color: Color; // user's color
  nbMoves: number; // number of moves it takes to get perfect score

  success: Assertion;
  failure?: Assertion;

  obstacles?: Key[]; // placed on the board, can be 'captured', offerIllegalDests by default for king collisions
  scenario?: UsiWithColor[];

  drawShapes?: VmEvaluation<DrawShape[]>;
  squareHighlights?: VmEvaluation<SquareHighlight[]>;

  text?: I18nKey; // helping text displayed on the board
  nextButton?: boolean; // wait for user to click next - allows user to review the solution
  offerIllegalDests?: boolean;
  showFailureMove?: 'random' | 'capture' | 'unprotected' | VmEvaluation<Usi | undefined>; // played after failure
}

export type IncompleteLevel = Omit<Level, 'id' | 'color'> & Partial<Pick<Level, 'color'>>;

export interface Stage {
  id: StageId;
  key: StageKey;
  title: I18nKey; // title to the right of the board
  subtitle: I18nKey; // below title
  intro: I18nKey; // overlay when stage starts
  complete: I18nKey; // overlay after stage is completed
  levels: Level[];
}

export type IncompleteStage = Omit<Stage, 'id'>;

export interface Category {
  key: CategoryKey;
  stages: Stage[];
}

export type StageState = 'init' | 'running' | 'completed' | 'end';
export type LevelState = 'play' | 'completed' | 'fail';

export interface Vm {
  category: Category;
  sideCategory: CategoryKey;
  stage: Stage;
  stageState: StageState;
  level: Level;
  levelState: LevelState;
  usiCList: UsiWithColor[];
  score?: Score;
}

export interface LearnOpts {
  data?: LearnProgress;
  pref: any;
  i18n: any;
}

export interface LearnProgress {
  _id?: string;
  stages: Record<string, ProgressScore>;
}

export interface ProgressScore {
  scores: Score[];
}

export type Redraw = () => void;
