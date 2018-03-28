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
    var url = 'https://github.com/gorhill/uBlock#installation';
    if (lichess.isChrome) url = 'https://chrome.google.com/webstore/detail/ublock-origin/cjpalhdlnbpafiamejdnhcphjbkeiagm';
    else if (navigator.userAgent.indexOf(' Firefox/') > -1) url = 'https://addons.mozilla.org/addon/ublock-origin/';
    else if (navigator.userAgent.indexOf(' Edge/') > -1) url = 'https://www.microsoft.com/store/p/app/9nblggh444l4';
    var html = '<a class="blocker button" href="'+url+'">' +
      '<img src="https://raw.githubusercontent.com/gorhill/uBlock/master/doc/img/icon38@2x.png" width=76 height=76 />' +
      '<strong>Install a malware blocker!</strong>' +
      'This page may include ads or trackers.<br />' +
      'We recommend uBlock Origin.' +
      '</a>';
    if (lichess.needBlocker) $('#site_header').append(html);
  });
});
