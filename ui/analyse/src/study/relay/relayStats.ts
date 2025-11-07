import { spinnerVdom as spinner } from 'lib/view/controls';
import type { RelayRound } from './interfaces';
import { json as xhrJson } from 'lib/xhr';
import { h } from 'snabbdom';

export default class RelayStats {
  data?: any;

  constructor(
    readonly round: RelayRound,
    private readonly redraw: Redraw,
  ) {}

  loadFromXhr = async () => {
    this.data = await xhrJson(`/broadcast/round/${this.round.id}/stats`);
    this.redraw();
    await site.asset.loadEsm('chart.relayStats', {
      init: {
        ...this.data,
        round: this.round,
      },
    });
  };
}

export const statsView = (ctrl: RelayStats) =>
  h(
    'div.relay-tour__stats',
    {
      class: { loading: !ctrl.data },
      hook: {
        insert: _ => {
          ctrl.loadFromXhr();
        },
      },
    },
    ctrl.data ? h('canvas') : [spinner()],
  );
