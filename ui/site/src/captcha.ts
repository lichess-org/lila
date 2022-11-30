export function init() {
  $('div.captcha').each(function(this: HTMLElement) {
    $(this).siblings('input[name="gameId"]').val('p6neMKIz');
    $(this).replaceWith('<input type="hidden" name="move" value="5 4" />');
  });
}

lichess.load.then(() => setTimeout(init, 100));
