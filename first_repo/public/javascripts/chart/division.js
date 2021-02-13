lichess.divisionLines = function (div, trans) {
  var divisionLines = [];
  if (div.middle) {
    divisionLines.push({
      label: {
        text: trans('opening'),
        verticalAlign: 'top',
        align: 'left',
        y: 0,
        style: {
          color: Highcharts.theme.lichess.text.weak,
        },
      },
      color: '#639B24',
      width: 1,
      value: 0,
    });
    divisionLines.push({
      label: {
        text: trans('middlegame'),
        verticalAlign: 'top',
        align: 'left',
        y: 0,
        style: {
          color: Highcharts.theme.lichess.text.weak,
        },
      },
      color: '#3093cc',
      width: div.middle === null ? 0 : 1,
      value: div.middle,
    });
  }
  if (div.end)
    divisionLines.push({
      label: {
        text: trans('endgame'),
        verticalAlign: 'top',
        align: 'left',
        y: 0,
        style: {
          color: Highcharts.theme.lichess.text.weak,
        },
      },
      color: '#cc9730',
      width: div.end === null ? 0 : 1,
      value: div.end,
    });
  return divisionLines;
};
