import { practiceComplete } from '../studyXhr';
import { type Prop, prop } from 'lib';
import { storedBooleanProp } from 'lib/storage';
import makeSuccess from './studyPracticeSuccess';
import { readOnlyProp } from '../../util';
import type { StudyPracticeData, Goal } from './interfaces';
import type { StudyData } from '../interfaces';
import type AnalyseCtrl from '../../ctrl';

export default class StudyPracticeCtrl {
  goal: Prop<Goal>;
  nbMoves = prop(0);
  // null = ongoing, true = win, false = fail
  success = prop<boolean | null>(null);
  autoNext = storedBooleanProp('analyse.practice-auto-next', true);

  constructor(
    readonly root: AnalyseCtrl,
    readonly studyData: StudyData,
    readonly data: StudyPracticeData,
  ) {
    this.goal = prop<Goal>(root.data.practiceGoal!);
    site.sound.load('practiceSuccess', site.sound.url('other/energy3.mp3'));
    site.sound.load('practiceFailure', site.sound.url('other/failure2.mp3'));
    this.onLoad();
  }

  onLoad = () => {
    this.root.showBestMoveArrows = readOnlyProp(true);
    this.root.showGauge = readOnlyProp(true);
    this.root.showFishnetAnalysis = readOnlyProp(true);
    this.goal(this.root.data.practiceGoal!);
    this.nbMoves(0);
    this.success(null);
  };

  computeNbMoves = (): number => {
    let plies = this.root.node.ply - this.root.tree.root.ply;
    if (this.root.bottomColor() !== this.root.data.player.color) plies--;
    return Math.ceil(plies / 2);
  };

  checkSuccess = (): void => {
    const gamebook = this.root.study?.gamebookPlay;
    if (gamebook) {
      if (gamebook.state.feedback === 'end') this.onVictory();
      return;
    }
    if (this.success() !== null || !this.root.practice || !this.root.study?.data.chapter.practice) return;
    this.nbMoves(this.computeNbMoves());
    const res = this.success(makeSuccess(this.root, this.goal(), this.nbMoves()));
    if (res) this.onVictory();
    else if (res === false) this.onFailure();
  };

  onVictory = (): void => {
    site.sound.play('practiceSuccess');
    this.onComplete();
    if (this.studyData.chapter.practice && this.autoNext())
      setTimeout(this.root.study!.goToNextChapter, 1000);
  };

  onComplete = (): void => {
    this.saveNbMoves();
  };

  saveNbMoves = (): void => {
    const chapterId = this.root.study!.currentChapter().id,
      former = this.data.completion[chapterId];
    if (typeof former === 'undefined' || this.nbMoves() < former) {
      this.data.completion[chapterId] = this.nbMoves();
      practiceComplete(chapterId, this.nbMoves());
    }
  };

  onFailure = (): void => {
    this.root.node.fail = true;
    site.sound.play('practiceFailure');
  };

  onJump = () => {
    // reset failure state if no failed move found in mainline history
    if (this.success() === false && !this.root.nodeList.find(n => !!n.fail)) this.success(null);
    this.checkSuccess();
  };
  onCeval = this.checkSuccess;
  reset = () => {
    this.root.tree.root.children = [];
    this.root.userJump('');
    this.root.practice!.reset();
    this.onLoad();
    this.root.practice!.resume();
  };
  isWhite = this.root.bottomIsWhite;
  analysisUrl = () =>
    `/analysis/standard/${this.root.node.fen.replace(/ /g, '_')}?color=${this.root.bottomColor()}`;
}
