lichess.divisionLines = function(div) {
  var divisionLines = [];
  if (div.middle) {
    divisionLines.push({
      label: {
        text: 'Opening',
        verticalAlign: 'top',
        align: 'left',
        y: 0,
        style: {
          color: Highcharts.theme.lichess.text.weak
        }
      },
      color: '#30cc4d',
      width: 1,
      value: 0
    });
    divisionLines.push({
      label: {
        text: 'Middlegame',
        verticalAlign: 'top',
        align: 'left',
        y: 0,
        style: {
          color: Highcharts.theme.lichess.text.weak
        }
      },
      color: '#3093cc',
      width: div.middle === null ? 0 : 1,
      value: div.middle
    });
  }
  if (div.end) divisionLines.push({
    label: {
      text: 'End-Game',
      verticalAlign: 'top',
      align: 'left',
      y: 0,
      style: {
        color: Highcharts.theme.lichess.text.weak
      }
    },
    color: '#cc9730',
    width: div.end === null ? 0 : 1,
    value: div.end
  });
  return divisionLines;
};
