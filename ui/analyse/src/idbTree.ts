import type AnalyseCtrl from './ctrl';
import { objectStorage, type ObjectStorage } from 'lib/objectStorage';

export type DiscloseState = undefined | 'expanded' | 'collapsed';

export class IdbTree {
  private dirty = false;
  private moveDb?: ObjectStorage<MoveState>;
  private collapseDb?: ObjectStorage<Tree.Path[]>;

  constructor(private ctrl: AnalyseCtrl) {}

  onAddNode(node: Tree.Node, path: Tree.Path): void {
    if (this.ctrl.study || this.ctrl.synthetic || this.dirty) return;
    this.dirty = !this.ctrl.tree.pathExists(path + node.id);
  }

  clear = async (): Promise<void> => {
    await this.collapseDb?.remove(this.id);
    if (!this.ctrl.study && !this.ctrl.synthetic) await this.moveDb?.put(this.id, { root: undefined });
    site.reload();
  };

  async saveMoves(force = false): Promise<IDBValidKey | undefined> {
    if (this.ctrl.study || this.ctrl.synthetic || !(this.dirty || force)) return;
    return this.moveDb?.put(this.id, { root: this.ctrl.tree.root });
  }

  async merge(): Promise<void> {
    if (!('indexedDB' in window) || !window.indexedDB) return;
    try {
      if (!this.ctrl.study && !this.ctrl.synthetic) {
        this.moveDb ??= await objectStorage<MoveState>({ store: 'analyse-state', db: 'lichess' });
        const state = await this.moveDb.get(this.ctrl.data.game.id);
        if (state?.root) {
          this.ctrl.tree.merge(state.root);
          this.dirty = true;
        }
      }
    } catch (e) {
      console.log('IDB error.', e);
    }
  }

  get isDirty(): boolean {
    return this.dirty;
  }

  private get id(): string {
    return this.ctrl.study?.data.chapter.id ?? this.ctrl.data.game.id;
  }
}

interface MoveState {
  root: Tree.Node | undefined;
}
