$(function() {
  $('button.follow').click(function() {
    var klass = 'active';
    $(this).toggleClass(klass);
    $.ajax({
      url: '/rel/' + ($(this).hasClass('active') ? 'follow/' : 'unfollow/') + $(this).data('user'),
      method:'post'
    });
  });
  setTimeout(function() {
    if (!lichess.needBlocker) return;
    var url = 'https://github.com/gorhill/uBlock#installation';
    if (lichess.isChrome) url = 'https://chrome.google.com/webstore/detail/ublock-origin/cjpalhdlnbpafiamejdnhcphjbkeiagm';
    else if (navigator.userAgent.indexOf(' Firefox/') > -1) url = 'https://addons.mozilla.org/addon/ublock-origin/';
    else if (navigator.userAgent.indexOf(' Edge/') > -1) url = 'https://www.microsoft.com/store/p/app/9nblggh444l4';
    $('#site_header').append('<a class="blocker button" href="'+url+'">' +
      '<i data-icon=""></i>' +
      '<strong>Install a malware blocker!</strong>' +
      'Be safe from ads and trackers<br />' +
      'infesting Twitch and YouTube.<br />' +
      'We recommend uBlock Origin<br />' +
      'which is free and open-source.' +
      '</a>');
  }, 1000);
});
