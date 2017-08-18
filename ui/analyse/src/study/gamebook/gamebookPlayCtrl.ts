import AnalyseCtrl from '../../ctrl';
import { StudyCtrl } from '../interfaces';

export default class GamebookPlayCtrl {

  ply: Ply;

  private mascots = [
    'octopus',
    'parrot-head',
    'ram',
    'camel-head',
    'owl'
  ];
  private mascotStorage = window.lichess.storage.make('gamebook.mascot');
  mascot = this.mascotStorage.get() || this.mascots[0];

  constructor(readonly root: AnalyseCtrl, readonly chapterId: string, readonly redraw: () => void) {
    this.ply = this.root.node.ply;
  }

  switchMascot = () => {
    const newIndex = this.mascots.indexOf(this.mascot) + 1;
    this.mascot = this.mascots[newIndex % this.mascots.length];
    this.mascotStorage.set(this.mascot);
    this.redraw();
  }

  private study = (): StudyCtrl => this.root.study!;
}
