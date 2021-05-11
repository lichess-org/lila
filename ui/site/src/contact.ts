lichess.load.then(() => {
  window.location.href = location.hash || '#help-root';

  $('a.contact-email-obfuscated').one('click', function (this: HTMLLinkElement) {
    $(this).html('...');
    setTimeout(() => {
      const address = atob($(this).data('email'));
      $(this).html(address).attr('href', `mailto:${address}`);
    }, 300);
    return false;
  });
});
