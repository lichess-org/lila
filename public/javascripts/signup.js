$(function() {
  const $form = $('#signup_form');
  const $exists = $form.find('.username-exists');

  const usernameCheck = lichess.debounce(function() {
    const name = $username.val();
    if (name.length >= 3) $.ajax({
      method: 'GET',
      url: '/player/autocomplete',
      data: {
        term: name,
        exists: 1
      },
      success: function(res) {
        $exists.toggle(res);
      }
    });
  }, 300);

  $username = $form.find('input[name="username"]')
    .on('change keyup paste', function() {
      $exists.hide();
      usernameCheck();
    });

  const commonEmailDomains = 'gmail.com hotmail.com yahoo.com mail.ru yandex.ru outlook.com icloud.com web.de bk.ru gmx.de hotmail.fr yandex.com wp.pl live.com protonmail.com yahoo.fr hotmail.co.uk inbox.ru rambler.ru list.ru aol.com googlemail.com seznam.cz mail.com libero.it volny.cz laposte.net hotmail.it orange.fr abv.bg o2.pl ukr.net naver.com yahoo.com.br outlook.fr yahoo.es msn.com interia.pl yahoo.de lichess.org me.com t-online.de ymail.com hotmail.es yahoo.co.uk hotmail.de ya.ru gmx.net sanluis.edu.ar gmx.com live.fr free.fr outlook.es gmx.at yahoo.it mynet.com onet.pl qq.com comcast.net op.pl windowslive.com live.nl yahoo.com.ar wanadoo.fr rediffmail.com yahoo.co.id virgilio.it outlook.de yahoo.co.in live.co.uk yahoo.gr bol.com.br inbox.lv rocketmail.com live.it gmx.fr freenet.de yahoo.ca alice.it sfr.fr btinternet.com tiscali.it mailfa.com sascholar.org live.de email.cz live.no bluewin.ch live.se gmx.ch freemail.hu outlook.it email.com hotmail.no i.ua live.ca netcourrier.com gmai.com sbcglobal.net hotmail.ca'.split(' ');

  $form.find('input[name="email"]')
    .on('change keyup paste', function() {
      $exists.hide();
      emailCheck();
    });


  $form.on('submit', function() {
    $form.find('button.submit')
      .attr('disabled', true)
      .removeAttr('data-icon')
      .addClass('frameless')
      .html(lichess.spinnerHtml);
  });
});
window.signupSubmit = function(token) {
  const form = document.getElementById('signup_form');
  if (form.reportValidity()) form.submit();
}
