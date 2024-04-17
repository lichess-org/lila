site.load.then(() => {
  $('.event .countdown').each(function () {
    if (!this.dataset.seconds) return;

    const $el = $(this);
    const seconds = parseInt(this.dataset.seconds) - 1;
    const target = new Date().getTime() + seconds * 1000;

    const second = 1000,
      minute = second * 60,
      hour = minute * 60,
      day = hour * 24;

    const redraw = function () {
      const distance = target - new Date().getTime();

      if (distance > 0) {
        $el.find('.days').text(Math.floor(distance / day).toString()),
          $el.find('.hours').text(Math.floor((distance % day) / hour).toString()),
          $el.find('.minutes').text(Math.floor((distance % hour) / minute).toString()),
          $el.find('.seconds').text(
            Math.floor((distance % minute) / second)
              .toString()
              .padStart(2, '0'),
          );
      } else {
        clearInterval(interval);
        site.reload();
      }
    };
    const interval = setInterval(redraw, second);

    redraw();
  });
});
