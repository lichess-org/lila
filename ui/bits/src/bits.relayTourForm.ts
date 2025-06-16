site.load.then(() => {
  $('.form3[action="/broadcast/new"] #form3-info_timeZone').each(function (this: HTMLSelectElement) {
    if (!$('.is-invalid').length)
      this.value = Intl.DateTimeFormat().resolvedOptions().timeZone;
  });
});
