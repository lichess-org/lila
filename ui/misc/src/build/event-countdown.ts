$('.event .countdown').each(function () {
  const $el = $(this),
    seconds = parseInt($(this).data('seconds')) - 1,
    target = new Date().getTime() + seconds * 1000;

  const second = 1000,
    minute = second * 60,
    hour = minute * 60,
    day = hour * 24;

  let interval: number;

  const redraw = () => {
    const distance = target - new Date().getTime();

    if (distance > 0) {
      $el.find('.days').text(Math.floor(distance / day)),
        $el.find('.hours').text(Math.floor((distance % day) / hour)),
        $el.find('.minutes').text(Math.floor((distance % hour) / minute)),
        $el.find('.seconds').text(Math.floor((distance % minute) / second));
    } else {
      clearInterval(interval);
      window.lishogi.reload();
    }
  };
  interval = setInterval(redraw, second);

  redraw();
});
