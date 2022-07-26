import { defined } from 'common';
import { objectStorage, ObjectStorage } from 'common/objectStorage';
import { storedBooleanProp } from 'common/storage';
import AnalyseCtrl from './ctrl';
import { AnalyseState } from './interfaces';

export default class Persistence {
  isDirty = false; // if isDirty show reset button to erase local move storage
  enabled = storedBooleanProp('save-moves-local-db', true);
  moveDb?: ObjectStorage<AnalyseState>;

  constructor(readonly ctrl: AnalyseCtrl) {}

  active = (): boolean => defined(this.moveDb) && this.enabled();

  toggle = (): boolean => {
    const saveMoves = !this.enabled();
    this.enabled(saveMoves);
    if (saveMoves) {
      if (this.isDirty) this.save();
      else this.merge();
    }
    this.ctrl.redraw();
    return saveMoves;
  };

  clear = (): void => {
    this.moveDb?.remove(this.ctrl.data.game.id);
    window.location.reload();
  };

  save(): void {
    if (this.moveDb && this.active() && this.isDirty) {
      this.moveDb.put(this.ctrl.data.game.id, {
        root: this.ctrl.tree.root,
        path: this.ctrl.path,
        flipped: this.ctrl.flipped,
      });
    }
  }

  async merge(): Promise<void> {
    if (this.ctrl.synthetic || this.ctrl.embed || this.ctrl.study) return;
    try {
      this.moveDb = await objectStorage<AnalyseState>('analyse-state');
      const state = await this.moveDb.get(this.ctrl.data.game.id);
      if (state && this.enabled()) {
        this.isDirty = true;
        this.ctrl.tree.merge(state.root);
        this.ctrl.jump(state.path);
        if (state.flipped != this.ctrl.flipped) this.ctrl.flip();
      }
      this.ctrl.redraw();
    } catch (e) {
      console.log(`IDB unavailable due to security settings or quota: ${e}`);
    }
  }
}
