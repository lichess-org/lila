export default function () {
  $('a.contact-email-obfuscated').one('click', function (this: HTMLLinkElement) {
    $(this).html('...');
    setTimeout(() => {
      const address = atob(this.dataset.email!);
      $(this).html(address).attr('href', `mailto:${address}`);
    }, 300);
    return false;
  });
}
