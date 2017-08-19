import AnalyseCtrl from '../../ctrl';
import { StudyCtrl } from '../interfaces';
import { readOnlyProp } from '../../util';
import Mascot from './mascot';

export default class GamebookPlayCtrl {

  ply: Ply;
  mascot = new Mascot();

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

  private study = (): StudyCtrl => this.root.study!;
}
