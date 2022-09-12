interface Division {
  middle?: number;
  end?: number;
}

export default function (div: Division, trans: Trans, colorful: boolean) {
  const lines = [];
  lines.push({
    color: colorful ? '#00000000' : '#d85000', // not used for movetimes or mods
    width: 1,
    value: 0,
  });
  if (div.middle) {
    lines.push({
      label: {
        text: trans('opening'),
        verticalAlign: 'top',
        align: 'left',
        y: 0,
        style: {
          color: window.Highcharts.theme.lichess.text.weak,
        },
      },
      color: colorful ? '#639B24' : window.Highcharts.theme.lichess.text.weak,
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
          color: window.Highcharts.theme.lichess.text.weak,
        },
      },
      color: colorful ? '#3093cc' : window.Highcharts.theme.lichess.text.weak,
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
          color: window.Highcharts.theme.lichess.text.weak,
        },
      },
      color: colorful ? '#cc9730' : window.Highcharts.theme.lichess.text.weak,
      width: div.end === null ? 0 : 1,
      value: div.end,
    });
  return lines;
}
