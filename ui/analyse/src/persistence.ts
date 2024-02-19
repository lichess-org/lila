import AnalyseCtrl from './ctrl';
import { AnalyseState } from './interfaces';
import { objectStorage, ObjectStorage } from 'common/objectStorage';

export default class Persistence {
  isDirty = false; // there are persisted user moves
  moveDb?: ObjectStorage<AnalyseState>;

  constructor(readonly ctrl: AnalyseCtrl) {}

  clear = (): void => {
    if (this.ctrl.synthetic) return;
    this.moveDb?.put(this.ctrl.data.game.id, {
      root: undefined,
      path: undefined,
      flipped: this.ctrl.flipped,
    });
    site.reload();
  };

  autosave(): void {
    this.moveDb?.put('autoload', {
      root: this.ctrl.tree.root,
      path: this.ctrl.path,
      flipped: this.ctrl.flipped,
    });
  }

  save(force = false): void {
    if (this.ctrl.synthetic || !(this.isDirty || force)) return;
    this.moveDb?.put(this.ctrl.data.game.id, {
      root: this.ctrl.tree.root,
      path: this.ctrl.path,
      flipped: this.ctrl.flipped,
    });
  }

  onAddNode(node: Tree.Node, path: Tree.Path) {
    if (this.ctrl.synthetic || this.isDirty) return;
    this.isDirty = !this.ctrl.tree.pathExists(path + node.id);
  }

  async merge(): Promise<void> {
    try {
      this.moveDb = await objectStorage<AnalyseState>({ store: 'analyse-state', db: 'lichess' });
      const state = await this.moveDb.get(this.ctrl.synthetic ? 'autoload' : this.ctrl.data.game.id);
      if (state?.root && state?.path) {
        this.ctrl.tree.merge(state.root);
        if (!this.ctrl.ongoing) this.ctrl.jump(state.path);
        if (this.ctrl.synthetic) this.moveDb?.remove('autoload');
        else this.isDirty = true;
      }
      if (state?.flipped === !this.ctrl.flipped) this.ctrl.flip();
      this.ctrl.redraw();
    } catch (e) {
      console.log('IDB unavailable.', e);
    }
  }
}
