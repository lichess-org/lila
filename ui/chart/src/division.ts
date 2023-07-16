import { Division } from './interface';

const divisionLine = (division: string, ply: number) => {
  const textWeak = window.Highcharts.theme.lichess.text.weak;
  return {
    label: {
      text: division,
      verticalAlign: 'top',
      align: 'left',
      y: 0,
      style: {
        color: textWeak,
      },
    },
    color: textWeak,
    width: 1,
    value: ply,
    zIndex: 5,
  };
};

export default function (div: Division | undefined, trans: Trans) {
  const lines = [];
  lines.push({
    color: window.Highcharts.theme.lichess.line.accent,
    width: 1,
    value: 0,
    zIndex: 6,
  });
  if (div?.middle) {
    if (div.middle > 1) lines.push(divisionLine(trans('opening'), 0));
    lines.push(divisionLine(trans('middlegame'), div.middle - 1));
  }
  if (div?.end) {
    if (div.end > 1 && !div?.middle) lines.push(divisionLine(trans('middlegame'), 0));
    lines.push(divisionLine(trans('endgame'), div.end - 1));
  }
  return lines;
}
