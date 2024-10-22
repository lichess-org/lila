export const addPasswordVisibilityToggleListener = (): void => {
  $('.password-wrapper').each(function (this: HTMLElement) {
    const $wrapper = $(this);
    const $button = $wrapper.find('.password-reveal');
    $button.on('click', function (e: Event) {
      e.preventDefault();
      const $input = $wrapper.find('input');
      const type = $input.attr('type') === 'password' ? 'text' : 'password';
      $input.attr('type', type);
      $button.toggleClass('revealed', type == 'text');
    });
  });
};
