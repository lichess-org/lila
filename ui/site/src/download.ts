function generateURL() {
  function addParameter(name: string, value: string | number | boolean): void {
    if (!url.endsWith('?')) url += '&';
    url += `${name}=${value}`;
  }

  let url: string =
    'https://lichess.org/api/games/user/' +
    encodeURIComponent((document.getElementById('dl-username') as HTMLInputElement).value) +
    '?';

  {
    const minTimestamp = 1356998400070;
    const midnight = '00:00:00';
    const dateMin = (document.getElementById('dl-dateMin') as HTMLInputElement).value;
    const timeMin = (document.getElementById('dl-timeMin') as HTMLInputElement).value;
    if (dateMin.length == 10) {
      // the 00:00:00 is necessary for the time to be interpreted in the local timezone
      const since = new Date(`${dateMin} ${timeMin.length == 8 ? timeMin : midnight}`);
      addParameter('since', Math.max(since.getTime(), minTimestamp));
    }

    const dateMax = (document.getElementById('dl-dateMax') as HTMLInputElement).value;
    const timeMax = (document.getElementById('dl-timeMax') as HTMLInputElement).value;
    if (dateMax.length == 10) {
      const until = new Date(`${dateMax} ${timeMax.length == 8 ? timeMax : midnight}`);
      // If no time is specified, assume that all games on that day should be included
      if (timeMax.length != 8) until.setDate(until.getDate() + 1);
      addParameter('until', Math.max(until.getTime(), minTimestamp));
    }
  }

  {
    const max = (document.getElementById('dl-amount') as HTMLInputElement).value;
    if (max.length > 0) addParameter('max', max);

    const vs = (document.getElementById('dl-opponent') as HTMLInputElement).value;
    if (vs.length > 0) addParameter('vs', encodeURIComponent(vs));

    const rated = (document.getElementById('dl-rated') as HTMLSelectElement).value;
    if (rated.length > 0) addParameter('rated', rated);
  }

  {
    const perfTbl = document.getElementById('dl-perf-tbl') as HTMLTableElement;
    const toggles = perfTbl.getElementsByClassName('cmn-toggle') as HTMLCollectionOf<HTMLInputElement>;
    const perfs: Array<string> = [];
    for (const toggle of toggles) {
      if (toggle.checked) perfs.push(toggle.value);
    }
    // don't add parameter if all or no perf types are selected
    if (perfs.length > 0 && perfs.length < 14) addParameter('perfType', perfs.join(','));
  }

  {
    const color = (document.getElementById('dl-color') as HTMLSelectElement).value;
    if (color.length > 0) addParameter('color', color);

    const analysed = (document.getElementById('dl-analysis') as HTMLSelectElement).value;
    if (analysed.length > 0) addParameter('analysed', analysed);

    const ongoing = (document.getElementById('dl-ongoing') as HTMLInputElement).checked;
    if (ongoing) addParameter('ongoing', ongoing);

    const tags = (document.getElementById('dl-tags') as HTMLInputElement).checked;
    if (!tags) addParameter('tags', tags);

    const clocks = (document.getElementById('dl-clocks') as HTMLInputElement).checked;
    if (clocks) addParameter('clocks', clocks);

    const evals = (document.getElementById('dl-evals') as HTMLInputElement).checked;
    if (evals) addParameter('evals', evals);

    const opening = (document.getElementById('dl-opening') as HTMLInputElement).checked;
    if (opening) addParameter('opening', opening);
  }

  const output = document.getElementById('dl-output') as HTMLInputElement;
  output.value = url;
  output.style.visibility = 'visible';
}

(document.getElementById('dl-button') as HTMLButtonElement).onclick = generateURL;
