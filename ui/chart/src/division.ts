interface Division {
  middle?: number;
  end?: number;
}

export default function (div: Division, trans: Trans, colorful: boolean) {
  // plotLines[0] reserved for current ply indicator but unused for movetimes graph or cheat hunter view
  const lines = [];
  lines.push({
    color: '#d85000',
    width: 1,
    value: 0,
  });
  const textWeak = window.Highcharts.theme.lichess.text.weak;
  if (div.middle) {
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
      color: colorful ? '#639B24' : textWeak,
      width: 1,
      value: 0,
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
      color: colorful ? '#3093cc' : textWeak,
      width: div.middle === null ? 0 : 1,
      value: div.middle,
    });
  }
  if (div.end)
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
      color: colorful ? '#cc9730' : textWeak,
      width: div.end === null ? 0 : 1,
      value: div.end,
    });
  return lines;
}
