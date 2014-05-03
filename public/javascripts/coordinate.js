$(function() {
  $('#coordinate_trainer').each(function() {
    var $trainer = $(this);
    var $board = $trainer.find('div.lichess_board');
    var $bar = $('#progress_bar');
    var $coord = $('#next_coordinate');
    var duration = 30*1000;
    var tickDelay = 50;
    var coordToGuess;

    var newCoord = function() {
      coordToGuess = 'ABCDEFGH'[_.random(0,7)] + _.random(1, 8);
      $coord.text(coordToGuess);
    };

    var done = function() {
      console.log('done!');
    };

    var tick = function() {
      var spent = Math.min(duration, (new Date() - startAt));
      $bar.css('width', (100 * spent / duration) + '%');
      if (spent < duration) setTimeout(tick, tickDelay);
      else done();
    };

    var startAt = new Date();
    $board.addClass('my_turn not_spectator').on('click', '.lcs', function() {
      var c = this.id;
      $bar.toggleClass('wrong', c != coordToGuess);
      newCoord();
    });
    newCoord();
    tick();
  });
});
