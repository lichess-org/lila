function generateURL() {
  const searchParams = new URLSearchParams();

  {
    const minTimestamp = 1356998400070;
    const midnight = '00:00:00';
    const dateMin = $('#dl-dateMin').val() as string;
    const timeMin = $('#dl-timeMin').val() as string;
    if (dateMin.length == 10) {
      // the 00:00:00 is necessary for the time to be interpreted in the local timezone
      const since = new Date(`${dateMin} ${timeMin.length == 8 ? timeMin : midnight}`);
      searchParams.append('since', Math.max(since.getTime(), minTimestamp).toString());
    }

    const dateMax = $('#dl-dateMax').val() as string;
    const timeMax = $('#dl-timeMax').val() as string;
    if (dateMax.length == 10) {
      const until = new Date(`${dateMax} ${timeMax.length == 8 ? timeMax : midnight}`);
      // If no time is specified, assume that all games on that day should be included
      if (timeMax.length != 8) until.setDate(until.getDate() + 1);
      searchParams.append('until', Math.max(until.getTime(), minTimestamp).toString());
    }
  }

  {
    const max = $('#dl-amount').val() as string;
    if (max.length > 0) searchParams.append('max', max);

    const vs = $('#dl-opponent').val() as string;
    if (vs.length > 0) searchParams.append('vs', encodeURIComponent(vs));

    const rated = $('#dl-rated').val() as string;
    if (rated.length > 0) searchParams.append('rated', rated);
  }

  {
    const perfs = $('#dl-perf-tbl .cmn-toggle')
      .get()
      .filter((e: HTMLInputElement) => e.checked)
      .map((e: HTMLInputElement): string => e.value);
    // don't add parameter if all or no perf types are selected
    if (perfs.length > 0 && perfs.length < 14) searchParams.append('perfType', perfs.join(','));
  }

  {
    const color = $('#dl-color').val() as string;
    if (color.length > 0) searchParams.append('color', color);

    const analysed = $('#dl-analysis').val() as string;
    if (analysed.length > 0) searchParams.append('analysed', analysed);

    const ongoing = $('#dl-ongoing').prop('checked') as boolean;
    if (ongoing) searchParams.append('ongoing', ongoing.toString());

    const tags = $('#dl-tags').prop('checked') as boolean;
    if (!tags) searchParams.append('tags', tags.toString());

    const clocks = $('#dl-clocks').prop('checked') as boolean;
    if (clocks) searchParams.append('clocks', clocks.toString());

    const evals = $('#dl-evals').prop('checked') as boolean;
    if (evals) searchParams.append('evals', evals.toString());

    const opening = $('#dl-opening').prop('checked') as boolean;
    if (opening) searchParams.append('opening', opening.toString());
  }

  const url =
    'https://lichess.org/api/games/user/' +
    encodeURIComponent($('#dl-username').val() as string) +
    '?' +
    searchParams.toString();
  $('#dl-output').val(url).css('visibility', 'visible');
}

$('#dl-button').on('click', generateURL);
