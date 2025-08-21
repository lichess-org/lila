import { formToXhr, text as xhrText } from 'lib/xhr';
import { debounce } from 'lib/async';
import * as licon from 'lib/licon';
import { sortTable, extendTablesortNumber } from 'lib/tablesort';
import { expandCheckboxZone, shiftClickCheckboxRange, selector } from './checkBoxes';
import { spinnerHtml } from 'lib/view/controls';
import { confirm } from 'lib/view/dialogs';
import { pubsub } from 'lib/pubsub';
import { commonDateFormat, toDate } from 'lib/i18n';
import { autolinkAtoms } from './mod.autolink';

site.load.then(() => {
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
        const prev = $zone.find(`.mz-section--${$(this).data('rel')}`);
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
    pubsub.emit('content-loaded', $inZone[0]);

    const makeReady = (selector: string, f: (el: HTMLElement, i: number) => void, cls = 'ready') => {
      $inZone.find(selector + `:not(.${cls})`).each(function (this: HTMLElement, i: number) {
        f($(this).addClass(cls)[0] as HTMLElement, i);
      });
    };

    const confirmButton = (el: HTMLElement) =>
      $(el)
        .find('input.confirm, button.confirm')
        .on('click', async function (this: HTMLElement, e: Event) {
          e.preventDefault();
          if (await confirm(this.title || 'Confirm this action?')) this.closest('form')?.submit();
        });

    $('.mz-section--menu > a:not(.available)').each(function (this: HTMLAnchorElement) {
      $(this).toggleClass('available', !!$(getLocationHash(this)).length);
    });
    makeReady('.mz-section--menu', el => {
      $(el)
        .find('a')
        .each(function (this: HTMLAnchorElement, i: number) {
          const id = getLocationHash(this),
            n = '' + (i + 1);
          $(this).prepend(`<i>${n}</i>`);
          site.mousetrap.bind(n, () => scrollTo(id));
        });
    });

    makeReady('form.xhr', (el: HTMLFormElement) => {
      confirmButton(el);
      $(el).on('submit', () => {
        $(el).addClass('ready').find('input').prop('disabled', true);
        formToXhr(el).then(html => {
          $zone.find('.mz-section--actions').replaceWith(html);
          userMod($inZone);
        });
        return false;
      });
    });
    makeReady('form.gdpr-erasure', confirmButton);

    makeReady('form.fide-title select', el =>
      $(el).on('change', () => ($(el).parent('form')[0] as HTMLFormElement).submit()),
    );

    makeReady('form.pm-preset select', (el: HTMLSelectElement) =>
      $(el).on('change', () => {
        const form = $(el).parent('form')[0] as HTMLFormElement;
        xhrText(form.getAttribute('action') + encodeURIComponent(el.value), { method: 'post' });
        $(form).html('Sent!');
      }),
    );

    makeReady('.mz-section--others', el => {
      $(el).height($(el).height());
    });
    makeReady('.mz-section--others table', (table: HTMLTableElement) => {
      sortTable(table, { descending: true });
      if (table) {
        expandCheckboxZone(table, 'td:last-child', shiftClickCheckboxRange(table));
        const select = table.querySelector('thead select');
        if (select)
          selector(
            table,
            select as HTMLSelectElement,
          )(async action => {
            if (action === 'alt') {
              const usernames = Array.from(
                $(table)
                  .find('td:last-child input:checked')
                  .map((_, input) => $(input).parents('tr').find('td:first-child').data('sort')),
              );
              if (usernames.length > 0 && (await confirm(`Close ${usernames.length} alt accounts?`))) {
                await xhrText('/mod/alt-many', { method: 'post', body: usernames.join(' ') });
                reloadZone();
              }
            }
          });
        if ($('#inquiry .notes').length) {
          $(table)
            .find('td.ips-prints')
            .addClass('add-to-note text')
            .attr('title', 'Add to note')
            .attr('data-icon', licon.Clipboard);
        }
      }
    });
    makeReady('.mz-section--identification .spy_filter', el => {
      $(el)
        .find('.button')
        .on('click', function (this: HTMLAnchorElement) {
          xhrText($(this).attr('href')!, { method: 'post' });
          $(this).parent().parent().toggleClass('blocked');
          return false;
        });
      let selected: string | undefined;
      const applyFilter = (v?: string) =>
        v
          ? $inZone.find('.mz-section--others tbody tr').each(function (this: HTMLElement) {
              $(this).toggleClass('none', !(this.dataset.tags || '').includes(v));
            })
          : $inZone.find('.mz-section--others tbody tr.none').removeClass('none');
      $(el)
        .find('tr')
        .on('click', function (this: HTMLTableRowElement) {
          const v = this.dataset.value;
          selected = selected === v ? undefined : v;
          applyFilter(selected);
          $('.spy_filter tr.selected').removeClass('selected');
          $(this).toggleClass('selected', !!selected);
        })
        .on('mouseenter', function (this: HTMLTableRowElement) {
          !selected && applyFilter(this.dataset.value);
        });
      $(el).on('mouseleave', () => !selected && applyFilter());
    });
    makeReady(
      '.mz-section--identification .slist--sort',
      el => {
        sortTable(el, { descending: true });
      },
      'ready-sort',
    );
    makeReady('.mz-section--others .more-others', el => {
      $(el)
        .addClass('ready')
        .on('click', () => {
          nbOthers = 1000;
          reloadZone();
        });
    });
    autolinkAtoms($inZone[0]);
  }

  const onScroll = () =>
    requestAnimationFrame(() => {
      if ($zone.hasClass('none')) return;
      $zone.toggleClass('stick-menu', window.scrollY > 200);
    });

  extendTablesortNumber();

  if (new URL(location.href).searchParams.has('mod')) $toggle.trigger('click');

  site.mousetrap
    .bind('m', () => $toggle.trigger('click'))
    .bind('i', () => $zone.find('button.inquiry').trigger('click'));

  const $other = $('#communication,main.appeal');
  if ($other.length) userMod($other);

  const timelineFlairDateToLocal = (el?: HTMLElement | undefined) =>
    $(el || document.body)
      .find('.mod-timeline__event__flair img[datetime]')
      .each(function (this: HTMLImageElement) {
        this.title += ' ' + commonDateFormat(toDate(this.getAttribute('datetime')!));
        this.removeAttribute('datetime');
      });
  timelineFlairDateToLocal();
  pubsub.on('content-loaded', timelineFlairDateToLocal);
});
