var tablesort = require('tablesort');

let $toggle = $('.user-show .mod-zone-toggle');
let $zone = $('.user-show .mod-zone');
let nbOthers = 50;

function streamLoad() {
  const source = new EventSource($toggle.attr('href') + '?nbOthers=' + nbOthers);
  const callback = lichess.debounce(() => userMod($zone), 300);
  source.addEventListener('message', e => {
    if (!e.data) return;
    const html = $('<output>').append($.parseHTML(e.data));
    html.find('.mz-section').each(function() {
      const prev = $('#' + this.id);
      if (prev.length) prev.replaceWith($(this));
      else $zone.append($(this).clone());
    });
    callback();
  });
  source.onerror = () => source.close();
}

function loadZone() {
  $zone.html(lichess.spinnerHtml).removeClass('none');
  $('#main-wrap').addClass('full-screen-force');
  $zone.html('');
  streamLoad();
  window.addEventListener('scroll', onScroll);
  scrollTo('.mod-zone');
}
function unloadZone() {
  $zone.addClass('none');
  $('#main-wrap').removeClass('full-screen-force');
  window.removeEventListener('scroll', onScroll);
  scrollTo('#top');
}
function reloadZone() {
  streamLoad();
}

function scrollTo(el) {
  window.scrollTo(0, document.querySelector(el).offsetTop);
}

$toggle.click(function() {
  if ($zone.hasClass('none')) loadZone();
  else unloadZone();
  return false;
});

function userMod($zone) {

  lichess.pubsub.emit('content_loaded');

  $('#mz_menu > a:not(.available)').each(function() {
    $(this).toggleClass('available', !!$($(this).attr('href')).length);
  });
  $('#mz_menu > a:not(.hotkey)').each(function(i) {
    const id = this.href.replace(/.+(#\w+)$/, '$1'), n = '' + (i + 1);
    $(this).addClass('hotkey').prepend(`<i>${n}</i>`);
    Mousetrap.bind(n, () => {
      console.log(id, n);
      scrollTo(id);
    });
  });

  $zone.find('form.xhr:not(.ready)').submit(function() {
    $(this).addClass('ready').find('input').attr('disabled', true);
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

  $zone.find('#mz_others table').each(function() {
    tablesort(this, {
      descending: true
    });
  });
  $zone.find('#mz_others .more-others:not(.ready)').each(function() {
    $(this).addClass('.ready').click(() => {
      nbOthers = 1000;
      reloadZone();
    });
  });
}

const onScroll = e => requestAnimationFrame(() => {
  if ($zone.hasClass('none')) return;
  $zone.toggleClass('stick-menu', window.scrollY > 200);
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
    return item.match(/^[-+]?(\d)*-?([,\.]){0,1}-?(\d)+([E,e][\-+][\d]+)?%?$/); // Number
  }, function(a, b) {
    return compareNumber(cleanNumber(b), cleanNumber(a));
  });
}());

if (location.search.startsWith('?mod')) $toggle.click();
Mousetrap.bind('m', () => $toggle.click());
