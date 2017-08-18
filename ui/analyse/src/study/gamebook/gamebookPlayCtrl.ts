import AnalyseCtrl from '../../ctrl';
import { StudyCtrl } from '../interfaces';
import { readOnlyProp } from '../../util';

export default class GamebookPlayCtrl {

  ply: Ply;

  private mascots = [
    'octopus',
    'parrot-head',
    'camel-head',
    'owl'
  ];
  private mascotStorage = window.lichess.storage.make('gamebook.mascot');
  mascot = this.mascotStorage.get() || this.mascots[0];

  constructor(readonly root: AnalyseCtrl, readonly chapterId: string, readonly redraw: () => void) {
    this.ply = this.root.node.ply;
    // root.showAutoShapes = readOnlyProp(true);
    // root.showGauge = readOnlyProp(true);
    // root.showComputer = readOnlyProp(true);
    // goal(root.data.practiceGoal!);
    // nbMoves(0);
    // success(null);
    // comment(makeComment(root.tree.root));
    // const chapter = studyData.chapter;
    // history.replaceState(null, chapter.name, data.url + '/' + chapter.id);
    // analysisUrl('/analysis/standard/' + root.node.fen.replace(/ /g, '_') + '?color=' + root.bottomColor());
  }

  switchMascot = () => {
    const newIndex = this.mascots.indexOf(this.mascot) + 1;
    this.mascot = this.mascots[newIndex % this.mascots.length];
    this.mascotStorage.set(this.mascot);
    this.redraw();
  }

  private study = (): StudyCtrl => this.root.study!;
}
