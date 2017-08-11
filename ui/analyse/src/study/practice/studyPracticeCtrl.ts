import * as xhr from '../studyXhr';
import { prop } from 'common';
import { enrichText } from '../studyComments';
import makeSuccess from './studyPracticeSuccess';
import makeSound from './sound';
import { PracticeData, Goal, PracticeCtrl } from './interfaces';
import { StudyData, StudyChapterMeta } from '../interfaces';
import AnalyseController from '../../ctrl';

function readOnlyProp<A>(value: A): () => A {
  return function(): A {
    return value;
  };
}

export default function(root: AnalyseController, studyData: StudyData, data: PracticeData): PracticeCtrl {

  const goal = prop<Goal>(root.data.practiceGoal!);
  const comment = prop<string | undefined>(undefined);
  const nbMoves = prop(0);
  // null = ongoing, true = win, false = fail
  const success = prop<boolean | null>(null);
  const sound = makeSound();
  const analysisUrl = prop('');

  function makeComment(treeRoot: Tree.Node): string | undefined {
    if (!treeRoot.comments) return;
    const c = enrichText(treeRoot.comments[0].text, false);
    delete treeRoot.comments;
    return c;
  };

  function onLoad() {
    root.showAutoShapes = readOnlyProp(true);
    root.showGauge = readOnlyProp(true);
    root.showComputer = readOnlyProp(true);
    goal(root.data.practiceGoal!);
    nbMoves(0);
    success(null);
    comment(makeComment(root.tree.root));
    const chapter = studyData.chapter;
    history.replaceState(null, chapter.name, data.url + '/' + chapter.id);
    analysisUrl('/analysis/standard/' + root.node.fen.replace(/ /g, '_') + '?color=' + root.bottomColor());
  };
  onLoad();

  function computeNbMoves(): number {
    let plies = root.node.ply - root.tree.root.ply;
    if (root.bottomColor() !== root.data.player.color) plies--;
    return Math.ceil(plies / 2);
  };

  function checkSuccess(): void {
    if (success() !== null) return;
    nbMoves(computeNbMoves());
    const res = success(makeSuccess(root, goal(), nbMoves()));
    if (res) onVictory();
    else if (res === false) onFailure();
  };

  function onVictory(): void {
    var chapterId = root.study.currentChapter().id;
    var former = data.completion[chapterId] || 999;
    if (nbMoves() < former) {
      data.completion[chapterId] = nbMoves();
      xhr.practiceComplete(chapterId, nbMoves());
    }
    sound.success();
    const next = nextChapter();
    if (next) setTimeout(() => root.study.setChapter(next.id), 1000);
  };

  function onFailure(): void {
    root.node.fail = true;
    sound.failure();
  };

  function nextChapter(): StudyChapterMeta | undefined {
    const chapters = root.study.data.chapters;
    const currentId = root.study.currentChapter().id;
    for (var i in chapters)
      if (chapters[i].id === currentId) return chapters[parseInt(i) + 1];
  };

  return {
    onReload: onLoad,
    onJump() {
      // reset failure state if no failed move found in mainline history
      if (success() === false && !root.nodeList.find(n => !!n.fail)) success(null);
      checkSuccess();
    },
    onCeval: checkSuccess,
    data,
    goal,
    success,
    comment,
    nbMoves,
    reset() {
      root.tree.root.children = [];
      root.userJump('');
      root.practice!.reset();
      onLoad();
    },
    isWhite() {
      return root.bottomColor() === 'white';
    },
    analysisUrl,
    nextChapter
  };
}
