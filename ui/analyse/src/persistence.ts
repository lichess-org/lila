import AnalyseCtrl from './ctrl';
import { AnalyseState } from './interfaces';
import { objectStorage, ObjectStorage } from 'common/objectStorage';

export default class Persistence {
  isDirty = false; // there are persisted user moves
  moveDb?: ObjectStorage<AnalyseState>;

  constructor(readonly ctrl: AnalyseCtrl) {}

  clear = (): void => {
    this.moveDb?.put(this.ctrl.data.game.id, {
      root: undefined,
      path: undefined,
      flipped: this.ctrl.flipped,
    });
    lichess.reload();
  };

  save(force = false): void {
    if (this.isDirty || force) {
      this.moveDb?.put(this.ctrl.data.game.id, {
        root: this.ctrl.tree.root,
        path: this.ctrl.path,
        flipped: this.ctrl.flipped,
      });
    }
  }

  onAddNode(node: Tree.Node, path: Tree.Path) {
    if (!this.isDirty) {
      this.isDirty = !this.ctrl.tree.pathExists(path + node.id);
    }
  }

  async merge(): Promise<void> {
    try {
      this.moveDb = await objectStorage<AnalyseState>({ store: 'analyse-state', db: 'lichess' });
      const state = await this.moveDb.get(this.ctrl.data.game.id);
      if (state?.root && state?.path) {
        this.ctrl.tree.merge(state.root);
        if (!this.ctrl.ongoing) this.ctrl.jump(state.path);
        this.isDirty = true;
      }
      if (state?.flipped === !this.ctrl.flipped) this.ctrl.flip();
      this.ctrl.redraw();
    } catch (e) {
      console.log('IDB unavailable.', e);
    }
  }
}
