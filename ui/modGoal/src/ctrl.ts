import { prop, Prop } from 'common';

export default class Ctrl {
  setup: Prop<boolean> = prop(false);
  constructor(readonly redraw: () => void) {}

  toggleSetup = () => {
    this.setup(!this.setup());
    this.redraw();
  };
}
