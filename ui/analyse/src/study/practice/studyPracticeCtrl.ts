import * as xhr from '../studyXhr';
import { Prop, prop } from 'common';
import { storedBooleanProp } from 'common/storage';
import makeSuccess from './studyPracticeSuccess';
import { readOnlyProp } from '../../util';
import { StudyPracticeData, Goal } from './interfaces';
import { StudyData } from '../interfaces';
import AnalyseCtrl from '../../ctrl';

export default class StudyPractice {
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
    this.root.showAutoShapes = readOnlyProp(true);
    this.root.variationArrowsProp = readOnlyProp(false);
    this.root.showGauge = readOnlyProp(true);
    this.root.showComputer = readOnlyProp(true);
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
    this.saveNbMoves();
    site.sound.play('practiceSuccess');
    if (this.studyData.chapter.practice && this.autoNext())
      setTimeout(this.root.study!.goToNextChapter, 1000);
  };

  saveNbMoves = (): void => {
    const chapterId = this.root.study!.currentChapter().id,
      former = this.data.completion[chapterId];
    if (typeof former === 'undefined' || this.nbMoves() < former) {
      this.data.completion[chapterId] = this.nbMoves();
      xhr.practiceComplete(chapterId, this.nbMoves());
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
