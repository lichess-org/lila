import type { TreePath } from 'lib/tree/types';

export default class StudyClockEdit {
  error: boolean = false;

  constructor(
    readonly slot: 'top' | 'bottom',
    readonly path: TreePath,
    public value: string,
    private redraw: Redraw,
  ) {}

  setValue = (value: string) => {
    this.value = value;
    this.redraw();
  };

  setEditError = (error: boolean) => {
    this.error = error;
    this.redraw();
  };
}
