import flatpickr from 'flatpickr';

site.load.then(() => {
  $('.flatpickr').each(function (this: HTMLInputElement) {
    const minDate = this.dataset['mindate'];
    const config = this.classList.contains('flatpickr-utc')
      ? {}
      : { dateFormat: 'Z', altInput: true, altFormat: 'Y-m-d h:i K' };
    flatpickr(this, {
      minDate: minDate == 'yesterday' ? new Date(Date.now() - 1000 * 3600 * 24) : minDate,
      maxDate: new Date(Date.now() + 1000 * 3600 * 24 * 31 * 12),
      monthSelectorType: 'static',
      disableMobile: true, // https://flatpickr.js.org/mobile-support/ https://github.com/lichess-org/lila/issues/8110
      ...config,
    });
  });
});
