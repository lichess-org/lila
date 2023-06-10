import AnalyseCtrl from './ctrl';
import { AnalyseState } from './interfaces';
import { defined, prop } from 'common';
import { objectStorage, ObjectStorage } from 'common/objectStorage';
import { storedBooleanProp } from 'common/storage';

export default class Persistence {
  isDirty = false; // there are persisted user moves
  open = prop(false);
  autoOpen = storedBooleanProp('analyse.persistence-auto-open', true);
  moveDb?: ObjectStorage<AnalyseState>;

  constructor(readonly ctrl: AnalyseCtrl) {}

  toggleOpen(v?: boolean) {
    this.open(defined(v) ? v : !this.open());
  }

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
      if (this.autoOpen()) this.open(true);
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
