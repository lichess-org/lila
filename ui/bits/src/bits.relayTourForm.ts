site.load.then(() => {
  $('#form3-info_timeZone').each(function (this: HTMLSelectElement) {
    const tz = Intl.DateTimeFormat().resolvedOptions().timeZone;
    this.value = tz;
  });
});
