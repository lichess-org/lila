import AnalyseCtrl from '../../../ctrl';
import { StudyCtrl } from '../../interfaces';

export default class GamebookPlayCtrl {

  root: AnalyseCtrl;

  chapterId: string;
  ply: Ply;

  constructor(root: AnalyseCtrl, chapterId: string) {
    this.root = root;
    this.chapterId = chapterId;
    this.ply = this.root.node.ply;
  }

  private study = (): StudyCtrl => this.root.study!;
}
