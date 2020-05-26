import {Data} from './interfaces';

export type Redraw = () => void

export class Ctrl {
  data: () => Data;
  trans: Trans;
  filter: string = "";
  showUserCreated: boolean = true;
  selectedPerf: Perf|null = null;

  constructor(readonly env: any, readonly redraw: Redraw) {
    this.data = () => env.data;
    this.trans = window.lichess.trans(env.i18n);
  }

  togglePerfVisibility(perf: Perf) {
    this.selectedPerf = this.selectedPerf === perf ? null : perf
  }
};