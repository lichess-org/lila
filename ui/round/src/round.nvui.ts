
import { type NvuiContext, makeContext } from 'lib/nvui/chess';
import { pubsub } from 'lib/pubsub';

import type RoundController from './ctrl';
import type { NvuiPlugin } from './interfaces';
import { renderNvui } from './view/nvuiView';

export type RoundNvuiContext = NvuiContext &
  Readonly<{
    ctrl: RoundController;
  }>;

export function initModule(ctrl: RoundController): NvuiPlugin {
  const ctx = makeContext<RoundNvuiContext>({
    ctrl,
  });

  pubsub.on('socket.in.message', line => {
    if (line.u === 'lichess') ctx.notify.set(line.t);
  });
  pubsub.on('round.suggestion', ctx.notify.set);

  const nvui: NvuiPlugin = {
    premoveInput: '',
    playPremove() {
      nvui.submitMove?.(true);
      nvui.premoveInput = '';
    },
    submitMove: undefined,
    render: () => renderNvui(ctx),
  };
  return nvui;
}

