import { h } from 'snabbdom';

export const renderClock = (color: Color, time: number) =>
  h(`span.mini-game__clock.mini-game__clock--${color}`, {
    attrs: {
      'data-time': time,
      'data-managed': 1,
    },
  });
