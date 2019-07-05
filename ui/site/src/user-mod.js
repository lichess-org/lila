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
  $zone.html(lichess.spinnerHtml).removeClass('none');
  streamLoad({
    node: $zone[0],
    url: $toggle.attr('href'),
    callback: lichess.debounce(function() {
      userMod($zone);
    }, 300)
  });
}

$toggle.click(function() {
  if ($zone.hasClass('none')) loadZone();
  else $zone.addClass('none');
  return false;
});
if (location.search.startsWith('?mod')) $toggle.click();

function userMod($zone) {

  lichess.pubsub.emit('content_loaded');

  $('#mz_menu .mz_plan').toggleClass('disabled', !$('#mz_plan').length);

  $zone.find('form.xhr').submit(function() {
    $(this).find('input').attr('disabled', true);
    $.ajax({
      url: $(this).attr('action'),
      method: $(this).attr('method'),
      success: function(html) {
        $('#mz_actions').replaceWith(html);
        userMod($zone);
      }
    })
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

  $zone.find('li.ip').slice(0, 3).filter(':not(.loaded)').addClass('loaded').each(function() {
    var $li = $(this);
    $(this).one('mouseover', function() {
      $.ajax({
        url: '/mod/ip-intel?ip=' + $(this).find('.address ip').text(),
        success: function(res) {
          $li.append($('<span class="intel">' + res + '% proxy</span>'));
        }
      });
    });
  });

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
      return item.match(/^-?[£\x24Û¢´€]?\d+\s*([,.]\d{0,2})/) || // Prefixed currency
      item.match(/^-?\d+\s*([,.]\d{0,2})?[£\x24Û¢´€]/) || // Suffixed currency
      item.match(/^-?(\d)*-?([,.]){0,1}-?(\d)+([E,e][-+][\d]+)?%?$/); // Number
    }, function(a, b) {
      a = cleanNumber(a);
      b = cleanNumber(b);

      return compareNumber(b, a);
    });
  }());

  $zone.find('#mz_others table').each(function() {
    tablesort(this);
  });
}
