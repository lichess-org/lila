function generateSearchParams(): string {
  const searchParams = new URLSearchParams();

  $('#dl-form')
    .find('input[name], select[name]')
    .each((_, e: HTMLInputElement | HTMLSelectElement) => {
      const val = e.type === 'checkbox' ? (e as HTMLInputElement).checked.toString() : e.value;
      if (val && e.name) searchParams.append(e.name, val);
    });

  ['since', 'until'].forEach(name => {
    const minTimestamp = 1356998400070;
    const date = $(`#dl-date-${name}`).val() as string;
    const time = $(`#dl-time-${name}`).val() as string;
    if (date.length == 10) {
      // the 00:00:00 is necessary for the time to be interpreted in the local timezone
      const datetime = new Date(`${date} ${time.length == 8 ? time : '00:00:00'}`);
      // Include all games played on date-until if no time is specified
      if (time.length != 8 && name === 'until') datetime.setDate(datetime.getDate() + 1);
      searchParams.append(name, Math.max(datetime.getTime(), minTimestamp).toString());
    }
  });

  const perfToggles = $('#dl-perfs input[type="checkbox"]');
  const perfTogglesChecked = perfToggles.filter(':checked').get() as HTMLInputElement[];
  // don't add parameter if all or no perf types are selected
  if (perfTogglesChecked.length > 0 && perfTogglesChecked.length < perfToggles.length)
    searchParams.append('perfType', perfTogglesChecked.map(e => e.value).join(','));

  return searchParams.toString();
}

function update() {
  const params = generateSearchParams();
  const apiUrl = $('#dl-api-url');
  apiUrl.val(`${window.location.protocol}//${window.location.host}${apiUrl.data('apiPath')}?${params}`);
  const btn = $('#dl-button');
  btn.prop('href', btn.prop('href').split('?')[0] + '?' + params);
}

$('#dl-form').find('input, select').on('change', update);

update();
