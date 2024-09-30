import { Redraw } from 'common/snabbdom';
import { spinnerVdom as spinner } from 'common/spinner';
import { RelayRound } from './interfaces';
import * as xhr from 'common/xhr';
import { h } from 'snabbdom';

export default class RelayStats {
  data?: any;

  constructor(
    readonly round: RelayRound,
    private readonly redraw: Redraw,
  ) {}

  loadFromXhr = async () => {
    this.data = await xhr.json(`/broadcast/round/${this.round.id}/stats`);
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
