import * as xhr from '../studyXhr';
import { prop } from 'common';
import { storedBooleanProp } from 'common/storage';
import makeSuccess from './studyPracticeSuccess';
import { readOnlyProp } from '../../util';
import { StudyPracticeData, Goal, StudyPracticeCtrl } from './interfaces';
import { StudyData } from '../interfaces';
import AnalyseCtrl from '../../ctrl';

export default function (
  root: AnalyseCtrl,
  studyData: StudyData,
  data: StudyPracticeData,
): StudyPracticeCtrl {
  const goal = prop<Goal>(root.data.practiceGoal!),
    nbMoves = prop(0),
    // null = ongoing, true = win, false = fail
    success = prop<boolean | null>(null),
    autoNext = storedBooleanProp('analyse.practice-auto-next', true);

  lichess.sound.load('practiceSuccess', `${lichess.sound.baseUrl}/other/energy3`);
  lichess.sound.load('practiceFailure', `${lichess.sound.baseUrl}/other/failure2`);

  function onLoad() {
    root.showAutoShapes = readOnlyProp(true);
    root.variationArrowsProp = readOnlyProp(false);
    root.showGauge = readOnlyProp(true);
    root.showComputer = readOnlyProp(true);
    goal(root.data.practiceGoal!);
    nbMoves(0);
    success(null);
    const chapter = studyData.chapter;
    history.replaceState(null, chapter.name, data.url + '/' + chapter.id);
  }
  onLoad();

  function computeNbMoves(): number {
    let plies = root.node.ply - root.tree.root.ply;
    if (root.bottomColor() !== root.data.player.color) plies--;
    return Math.ceil(plies / 2);
  }

  function checkSuccess(): void {
    const gamebook = root.study!.gamebookPlay();
    if (gamebook) {
      if (gamebook.state.feedback === 'end') onVictory();
      return;
    }
    if (!root.study!.data.chapter.practice) {
      return saveNbMoves();
    }
    if (success() !== null) return;
    nbMoves(computeNbMoves());
    const res = success(makeSuccess(root, goal(), nbMoves()));
    if (res) onVictory();
    else if (res === false) onFailure();
  }

  function onVictory(): void {
    saveNbMoves();
    lichess.sound.play('practiceSuccess');
    if (studyData.chapter.practice && autoNext()) setTimeout(root.study!.goToNextChapter, 1000);
  }

  function saveNbMoves(): void {
    const chapterId = root.study!.currentChapter().id,
      former = data.completion[chapterId];
    if (typeof former === 'undefined' || nbMoves() < former) {
      data.completion[chapterId] = nbMoves();
      xhr.practiceComplete(chapterId, nbMoves());
    }
  }

  function onFailure(): void {
    root.node.fail = true;
    lichess.sound.play('practiceFailure');
  }

  return {
    onLoad,
    onJump() {
      // reset failure state if no failed move found in mainline history
      if (success() === false && !root.nodeList.find(n => !!n.fail)) success(null);
      checkSuccess();
    },
    onCeval: checkSuccess,
    data,
    goal,
    success,
    nbMoves,
    reset() {
      root.tree.root.children = [];
      root.userJump('');
      root.practice!.reset();
      onLoad();
      root.practice!.resume();
    },
    isWhite: root.bottomIsWhite,
    analysisUrl: () => `/analysis/standard/${root.node.fen.replace(/ /g, '_')}?color=${root.bottomColor()}`,
    autoNext,
  };
}
