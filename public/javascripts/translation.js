$(function() {
  if ($('div.locale_menu').length > 0) {
    $('div.messages div.message').each(function() {
      if (!$(this).find('input').val()) {
        $(this).addClass('missing');
      }
    });
    $('div.locale_menu a').click(function() {
      $(this).parent().find('a').removeClass('active');
      $(this).addClass('active');
      $('div.messages div.message').show();
      if ($(this).hasClass('missing')) {
        $('div.messages div.message').not('.missing').hide();
      }
    });
    if ($('div.messages div.missing').length > 0) {
      $('div.locale_menu a.missing').click();
    }
  }

  var inputHash = function() {
    return Array.prototype.map.call(
      document.querySelectorAll('div.messages input'),
      function(el) {
        return el.value;
      }).join('');
  };
  var initialHash = inputHash();

  var beforeUnload = function(e) {
    if (initialHash !== inputHash()) {
      var msg = 'There are unsaved translations!';
      (e || window.event).returnValue = msg;
      return msg;
    }
  };

  window.addEventListener('beforeunload', beforeUnload);

  $('form.translation_form').on('submit', function() {
    window.removeEventListener('beforeunload', beforeUnload);
  });
});
