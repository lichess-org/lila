import * as xhr from 'common/xhr';
import debounce from 'common/debounce';
import extendTablesortNumber from './component/tablesort-number';
import spinnerHtml from './component/spinner';
import tablesort from 'tablesort';

lichess.load.then(() => {
  const $toggle = $('.mod-zone-toggle'),
    $zone = $('.mod-zone-full');
  let nbOthers = 100;

  function streamLoad() {
    const source = new EventSource($toggle.attr('href') + '?nbOthers=' + nbOthers),
      callback = debounce(() => userMod($zone), 300);
    source.addEventListener('message', e => {
      if (!e.data) return;
      const html = $('<output>').append($.parseHTML(e.data));
      html.find('.mz-section').each(function (this: HTMLElement) {
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
    scrollTo('.mod-zone-full');
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

  $toggle.on('click', () => {
    if ($zone.hasClass('none')) loadZone();
    else unloadZone();
    return false;
  });

  const getLocationHash = (a: HTMLAnchorElement) => a.href.replace(/.+(#\w+)$/, '$1');

  function userMod($inZone: Cash) {
    lichess.contentLoaded($inZone[0]);

    const makeReady = (selector: string, f: (el: HTMLElement, i: number) => void, cls = 'ready') => {
      $inZone.find(selector + `:not(.${cls})`).each(function (this: HTMLElement, i: number) {
        f($(this).addClass(cls)[0] as HTMLElement, i);
      });
    };

    $('#mz_menu > a:not(.available)').each(function (this: HTMLAnchorElement) {
      $(this).toggleClass('available', !!$(getLocationHash(this)).length);
    });
    makeReady('#mz_menu', el => {
      $(el)
        .find('a')
        .each(function (this: HTMLAnchorElement, i: number) {
          const id = getLocationHash(this),
            n = '' + (i + 1);
          $(this).prepend(`<i>${n}</i>`);
          window.Mousetrap.bind(n, () => scrollTo(id));
        });
    });

    makeReady('form.xhr', (el: HTMLFormElement) => {
      $(el)
        .find('input.confirm, button.confirm')
        .on('click', function (this: HTMLElement) {
          return confirm(this.title || 'Confirm this action?');
        });
      $(el).on('submit', () => {
        $(el).addClass('ready').find('input').prop('disabled', true);
        xhr.formToXhr(el).then(html => {
          $('#mz_actions').replaceWith(html);
          userMod($inZone);
        });
        return false;
      });
    });

    makeReady('form.fide-title select', el =>
      $(el).on('change', () => ($(el).parent('form')[0] as HTMLFormElement).submit())
    );

    makeReady('form.pm-preset select', (el: HTMLSelectElement) =>
      $(el).on('change', () => {
        const form = $(el).parent('form')[0] as HTMLFormElement;
        xhr.text(form.getAttribute('action') + encodeURIComponent(el.value), { method: 'post' });
        $(form).html('Sent!');
      })
    );

    makeReady('.mz_others', el => {
      $(el).height($(el).height());
      $(el)
        .find('.mark-alt')
        .on('click', function (this: HTMLAnchorElement) {
          if (confirm('Close alt account?')) {
            xhr.text(this.getAttribute('href')!, { method: 'post' });
            $(this).remove();
          }
        });
    });
    makeReady('.mz_others table', el => {
      tablesort(el, { descending: true });
    });
    makeReady('#mz_identification .spy_filter', el => {
      $(el)
        .find('.button')
        .on('click', function (this: HTMLAnchorElement) {
          xhr.text($(this).attr('href')!, { method: 'post' });
          $(this).parent().parent().toggleClass('blocked');
          return false;
        });
      let selected: string | undefined;
      const valueOf = (el: HTMLTableRowElement) => $(el).find('td:first-child').text();
      const applyFilter = (v?: string) =>
        v
          ? $inZone.find('.mz_others tbody tr').each(function (this: HTMLElement) {
              $(this).toggleClass('none', !($(this).data('tags') || '').includes(v));
            })
          : $inZone.find('.mz_others tbody tr.none').removeClass('none');
      $(el)
        .find('tr')
        .on('click', function (this: HTMLTableRowElement) {
          const v = valueOf(this);
          selected = selected == v ? undefined : v;
          applyFilter(selected);
          $('.spy_filter tr.selected').removeClass('selected');
          $(this).toggleClass('selected', !!selected);
        })
        .on('mouseenter', function (this: HTMLTableRowElement) {
          !selected && applyFilter(valueOf(this));
        });
      $(el).on('mouseleave', () => !selected && applyFilter());
    });
    makeReady(
      '#mz_identification .slist--sort',
      el => {
        tablesort(el, { descending: true });
      },
      'ready-sort'
    );
    makeReady('.mz_others .more-others', el => {
      $(el)
        .addClass('.ready')
        .on('click', () => {
          nbOthers = 1000;
          reloadZone();
        });
    });
  }

  const onScroll = () =>
    requestAnimationFrame(() => {
      if ($zone.hasClass('none')) return;
      $zone.toggleClass('stick-menu', window.scrollY > 200);
    });

  extendTablesortNumber();

  if (new URL(location.href).searchParams.has('mod')) $toggle.trigger('click');

  window.Mousetrap.bind('m', () => $toggle.trigger('click')).bind('i', () =>
    $zone.find('button.inquiry').trigger('click')
  );

  const $other = $('#communication,main.appeal');
  if ($other.length) userMod($other);
});
