function cooldown(node, dateTarget) {

  var second = 1000,
    minute = second * 60,
    hour = minute * 60,
    day = hour * 24;

  var redraw = function() {

    var now = new Date().getTime(), distance = dateTarget - now;

    if (distance > 0) {
      node.querySelector('.days').innerText = Math.floor(distance / (day)),
      node.querySelector('.hours').innerText = Math.floor((distance % (day)) / (hour)),
      node.querySelector('.minutes').innerText = Math.floor((distance % (hour)) / (minute)),
      node.querySelector('.seconds').innerText = Math.floor((distance % (minute)) / second);
    } else {
      clearInterval(interval);
      lidraughts.reload();
    }

  };
  var interval = setInterval(redraw, second);

  redraw();
}

$('#event p.when').each(function() {
  var dateTarget = new Date($(this).find('time').attr('datetime'));
  $(this).replaceWith($(
    '<ul class="countdown"><li><span class="days"></span>days</li><li><span class="hours"></span>Hours</li><li><span class="minutes"></span>Minutes</li><li><span class="seconds"></span>Seconds</li></ul>'
  ));
  cooldown($('#event .countdown')[0], dateTarget);
  lidraughts.loadCss('stylesheets/event-countdown.css');
})
