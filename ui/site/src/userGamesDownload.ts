function generateURL() {
  function addParameter(name: string, value: string | number | boolean): void {
    if (!url.endsWith('?')) url += '&';
    url += `${name}=${value}`;
  }

  let url: string = 'https://lichess.org/api/games/user/' + encodeURIComponent($('#dl-username').val() as string) + '?';

  {
    const minTimestamp = 1356998400070;
    const midnight = '00:00:00';
    const dateMin = $('#dl-dateMin').val() as string;
    const timeMin = $('#dl-timeMin').val() as string;
    if (dateMin.length == 10) {
      // the 00:00:00 is necessary for the time to be interpreted in the local timezone
      const since = new Date(`${dateMin} ${timeMin.length == 8 ? timeMin : midnight}`);
      addParameter('since', Math.max(since.getTime(), minTimestamp));
    }

    const dateMax = $('#dl-dateMax').val() as string;
    const timeMax = $('#dl-timeMax').val() as string;
    if (dateMax.length == 10) {
      const until = new Date(`${dateMax} ${timeMax.length == 8 ? timeMax : midnight}`);
      // If no time is specified, assume that all games on that day should be included
      if (timeMax.length != 8) until.setDate(until.getDate() + 1);
      addParameter('until', Math.max(until.getTime(), minTimestamp));
    }
  }

  {
    const max = $('#dl-amount').val() as string;
    if (max.length > 0) addParameter('max', max);

    const vs = $('#dl-opponent').val() as string;
    if (vs.length > 0) addParameter('vs', encodeURIComponent(vs));

    const rated = $('#dl-rated').val() as string;
    if (rated.length > 0) addParameter('rated', rated);
  }

  {
    const perfs = $('#dl-perf-tbl .cmn-toggle')
      .get()
      .filter((e: HTMLInputElement) => e.checked)
      .map((e: HTMLInputElement): string => e.value);
    // don't add parameter if all or no perf types are selected
    if (perfs.length > 0 && perfs.length < 14) addParameter('perfType', perfs.join(','));
  }

  {
    const color = $('#dl-color').val() as string;
    if (color.length > 0) addParameter('color', color);

    const analysed = $('#dl-analysis').val() as string;
    if (analysed.length > 0) addParameter('analysed', analysed);

    const ongoing = $('#dl-ongoing').prop('checked') as boolean;
    if (ongoing) addParameter('ongoing', ongoing);

    const tags = $('#dl-tags').prop('checked') as boolean;
    if (!tags) addParameter('tags', tags);

    const clocks = $('#dl-clocks').prop('checked') as boolean;
    if (clocks) addParameter('clocks', clocks);

    const evals = $('#dl-evals').prop('checked') as boolean;
    if (evals) addParameter('evals', evals);

    const opening = $('#dl-opening').prop('checked') as boolean;
    if (opening) addParameter('opening', opening);
  }

  $('#dl-output').val(url).css('visibility', 'visible');
}

$('#dl-button').on('click', generateURL);
