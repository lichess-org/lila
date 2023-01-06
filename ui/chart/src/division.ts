import { Division } from './interface';

export default function (div: Division | undefined, trans: Trans) {
  const lines = [];
  lines.push({
    color: window.Highcharts.theme.lichess.line.accent,
    width: 1,
    value: 0,
    zIndex: 6,
  });
  const textWeak = window.Highcharts.theme.lichess.text.weak;
  if (div?.middle) {
    lines.push({
      label: {
        text: trans('opening'),
        verticalAlign: 'top',
        align: 'left',
        y: 0,
        style: {
          color: textWeak,
        },
      },
      color: textWeak,
      width: 1,
      value: 0,
      zIndex: 5,
    });
    lines.push({
      label: {
        text: trans('middlegame'),
        verticalAlign: 'top',
        align: 'left',
        y: 0,
        style: {
          color: textWeak,
        },
      },
      color: textWeak,
      width: div.middle === null ? 0 : 1,
      value: div.middle,
      zIndex: 5,
    });
  }
  if (div?.end)
    lines.push({
      label: {
        text: trans('endgame'),
        verticalAlign: 'top',
        align: 'left',
        y: 0,
        style: {
          color: textWeak,
        },
      },
      color: textWeak,
      width: div.end === null ? 0 : 1,
      value: div.end,
      zIndex: 5,
    });
  return lines;
}
