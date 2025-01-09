window.lishogi.ready.then(() => {
  document.querySelectorAll('.flatpickr--init').forEach((el: HTMLInputElement) => {
    window.flatpickr(el, {
      maxDate: new Date(Date.now() + 1000 * 3600 * 24 * 31),
      dateFormat: 'Z',
      altInput: true,
      altFormat: 'Y-m-d h:i K',
      disableMobile: true,
      enableTime: true,
      time_24hr: true,
      locale: document.documentElement.lang as any,
      // formatDate: (date, format) => {
      //   if (format === 'U') return Math.floor(date.getTime()).toString();
      //   return formattedDate(date, utc());
      // },
      // parseDate: (dateString, format) => {
      //   console.log('parseDate', dateString, format, new Date(dateString));
      //   if (format === 'U') {
      //     return new Date(parseInt(dateString));
      //   }
      //   return new Date(dateString);
      // },
    });
  });
});
