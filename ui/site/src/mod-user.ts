import tablesort from 'tablesort';
import * as xhr from 'common/xhr';
import debounce from 'common/debounce';
import spinnerHtml from './component/spinner';
import extendTablesortNumber from './component/tablesort-number';

window.lichess.load.then(() => {

  const $toggle = $('.mod-zone-toggle'),
    $zone = $('.mod-zone');
  let nbOthers = 100;

  function streamLoad() {
    const source = new EventSource($toggle.attr('href') + '?nbOthers=' + nbOthers),
      callback = debounce(() => userMod($zone), 300);
    source.addEventListener('message', e => {
      if (!e.data) return;
      const html = $('<output>').append($.parseHTML(e.data));
      html.find('.mz-section').each(function(this: HTMLElement) {
        const prev = $('#' + this.id);
        if (prev.length) prev.replaceWith($(this));
        else $zone.append($(this).clone());
      });
      callback();
    });
    source.onerror = () => source.close();
  }

  function loadZone() {
    $zone.html(spinnerHtml).removeClass('none');
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

  function scrollTo(selector: string) {
    const target = document.querySelector(selector) as HTMLElement | null;
    if (target) {
      const offset = $('#inquiry').length ? -50 : 50;
      window.scrollTo(0, target.offsetTop + offset);
    }
  }

  $toggle.click(function() {
    if ($zone.hasClass('none')) loadZone();
    else unloadZone();
    return false;
  });

  function userMod($zone: JQuery) {

    window.lichess.pubsub.emit('content_loaded');

    $('#mz_menu > a:not(.available)').each(function(this: HTMLElement) {
      $(this).toggleClass('available', !!$($(this).attr('href')).length);
    });
    makeReady('#mz_menu', el => {
      $(el).find('a').each(function(this: HTMLAnchorElement, i: number) {
        const id = this.href.replace(/.+(#\w+)$/, '$1'), n = '' + (i + 1);
        $(this).prepend(`<i>${n}</i>`);
        window.Mousetrap.bind(n, () => scrollTo(id));
      });
    });

    makeReady('form.xhr', (el: HTMLFormElement) => {
      $(el).submit(() => {
        $(el).addClass('ready').find('input').prop('disabled', true);
        xhr.formToXhr(el).then(html => {
          $('#mz_actions').replaceWith(html);
          userMod($zone);
        });
        return false;
      });
    });

    makeReady('form.fide_title select', el => {
      $(el).on('change', function() {
        $(el).parent('form').submit();
      });
    });

    makeReady('#mz_others', el => {
      $(el).height($(el).height());
      $(el).find('.mark-alt').on('click', function(this: HTMLAnchorElement) {
        if (confirm('Close alt account?')) {
          xhr.text(this.getAttribute('href')!, { method: 'post' });
          $(this).remove();
        }
      });
    });
    makeReady('#mz_others table', el => {
      tablesort(el, { descending: true });
    });
    makeReady('#mz_identification .spy_filter', el => {
      $(el).find('.button').click(function(this: HTMLAnchorElement) {
        xhr.text(this.getAttribute('href')!, { method: 'post' });
        $(this).parent().parent().toggleClass('blocked');
        return false;
      });
      $(el).find('tr').on('mouseenter', function(this: HTMLElement) {
        const v = $(this).find('td:first').text();
        $('#mz_others tbody tr').each(function(this: HTMLElement) {
          $(this).toggleClass('none', !($(this).data('tags') || '').includes(v));
        });
      });
      $(el).on('mouseleave', () => $('#mz_others tbody tr').removeClass('none'));
    });
    makeReady('#mz_identification .slist--sort', el => {
      tablesort(el, { descending: true });
    }, 'ready-sort');
    makeReady('#mz_others .more-others', el => {
      $(el).addClass('.ready').click(() => {
        nbOthers = 1000;
        reloadZone();
      });
    });
  }

  function makeReady(selector: string, f: (el: HTMLElement, i: number) => void, cls: string = 'ready') {
    $zone.find(selector + `:not(.${cls})`).each(function(this: HTMLElement, i: number) {
      f($(this).addClass(cls)[0] as HTMLElement, i);
    });
  }

  const onScroll = () => requestAnimationFrame(() => {
    if ($zone.hasClass('none')) return;
    $zone.toggleClass('stick-menu', window.scrollY > 200);
  });

  extendTablesortNumber();

  if (location.search.startsWith('?mod')) $toggle.click();

  window.Mousetrap.bind('m', () => $toggle.click());
  window.Mousetrap.bind('i', () => $zone.find('button.inquiry').click());
});
