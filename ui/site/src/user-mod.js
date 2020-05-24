var tablesort = require('tablesort');

function streamLoad(opts) {
  var source = new EventSource(opts.url), first = true;
  source.addEventListener('message', function(e) {
    var newHtml = e.data;
    if (!newHtml) return;
    if (first) {
      first = false;
      opts.node.innerHTML = newHtml;
    } else {
      opts.node.innerHTML += newHtml;
    }
    opts.callback();
  });
  source.onerror = function() { source.close(); };
}

var $toggle = $('.user-show .mod-zone-toggle');
var $zone = $('.user-show .mod-zone');

function loadZone() {
  $('.user-show').css('overflow', 'visible'); // required for mz_menu to be displayed
  $zone.html(lichess.spinnerHtml).removeClass('none');
  $('#main-wrap').addClass('full-screen-force');
  streamLoad({
    node: $zone[0],
    url: $toggle.attr('href'),
    callback: lichess.debounce(function() {
      userMod($zone);
    }, 300)
  });
	window.addEventListener('scroll', onScroll);
}
function unloadZone() {
  $zone.addClass('none');
  $('#main-wrap').removeClass('full-screen-force');
	window.removeEventListener('scroll', onScroll);
}

$toggle.click(function() {
  if ($zone.hasClass('none')) loadZone();
  else unloadZone();
  return false;
});

function userMod($zone) {

  lichess.pubsub.emit('content_loaded');

  $('#mz_menu .mz_plan').toggleClass('disabled', !$('#mz_plan').length);

  $zone.find('form.xhr').submit(function() {
    $(this).find('input').attr('disabled', true);
    $.ajax({
      ...lichess.formAjax($(this)),
      success: function(html) {
        $('#mz_actions').replaceWith(html);
        userMod($zone);
      }
    });
    return false;
  });

  $zone.find('form.fide_title select').on('change', function() {
    $(this).parent('form').submit();
  });

  var $modLog = $zone.find('#mz_mod_log ul').children();

  if ($modLog.length > 20) {
    var list = $modLog.slice(20);
    list.addClass('modlog-hidden').hide()
      .first().before('<a id="modlog-show">Show all ' + $modLog.length + ' mod log entries...</a>');
      $zone.find('#modlog-show').click(function() {
        $zone.find('.modlog-hidden').show();
        $(this).remove();
      });
  }

  (function(){
    var cleanNumber = function(i) {
      return i.replace(/[^\-?0-9.]/g, '');
    },

    compareNumber = function(a, b) {
      a = parseFloat(a);
      b = parseFloat(b);

      a = isNaN(a) ? 0 : a;
      b = isNaN(b) ? 0 : b;

      return a - b;
    };

    tablesort.extend('number', function(item) {
      return item.match(/^[-+]?(\d)*-?([,\.]){0,1}-?(\d)+([E,e][\-+][\d]+)?%?$/); // Number
    }, function(a, b) {
      return compareNumber(cleanNumber(b), cleanNumber(a));
    });
  }());

  $zone.find('#mz_others table').each(function() {
    tablesort(this, {
      descending: true
    });
  });
}

const onScroll = e => requestAnimationFrame(() => {
  if ($zone.hasClass('none')) return;
  $zone.toggleClass('stick-menu', window.scrollY > 220);
});

if (location.search.startsWith('?mod')) $toggle.click();
