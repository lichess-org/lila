import type RoundController from './ctrl';
import { renderNvui } from './view/nvuiView';
import type { NvuiPlugin } from './interfaces';
import { type NvuiContext, makeContext } from 'lib/nvui/chess';
import { makeSetting, type Setting } from 'lib/nvui/setting';
import { pubsub } from 'lib/pubsub';
import { storage } from 'lib/storage';

export type RoundNvuiContext = NvuiContext &
  Readonly<{
    ctrl: RoundController;
    pageStyle: Setting<PageStyle>;
  }>;

export function initModule(ctrl: RoundController): NvuiPlugin {
  const ctx = makeContext<RoundNvuiContext>({
    ctrl,
    pageStyle: makeSetting<PageStyle>({
      choices: [
        ['actions-board', `${i18n.nvui.actions} ${i18n.site.board}`],
        ['board-actions', `${i18n.site.board} ${i18n.nvui.actions}`],
      ],
      default: 'actions-board',
      storage: storage.make('nvui.pageLayout'),
    }),
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

type PageStyle = 'board-actions' | 'actions-board';
