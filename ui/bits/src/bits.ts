import { text, formToXhr } from 'lib/xhr';
import flairPickerLoader from './flairPicker';
import { spinnerHtml } from 'lib/view';
import { wireCropDialog } from './crop';

// avoid node_modules and pay attention to imports here. we don't want to force people
// to download the entire toastui editor library just to do some light form processing.

export function initModule(args: { fn: string } & any): void {
  switch (args.fn) {
    case 'appeal':
      return appeal();
    case 'autoForm':
      return autoForm(args);
    case 'colorizeYesNoTable':
      return colorizeYesNoTable();
    case 'contact':
      return contact();
    case 'dailyFeed':
      return dailyFeed();
    case 'embedReasonToggle':
      return embedReasonToggle();
    case 'eventCountdown':
      return eventCountdown();
    case 'hcaptcha':
      return hcaptcha();
    case 'importer':
      return importer();
    case 'pmAll':
      return pmAll();
    case 'relayForm':
      return relayForm();
    case 'setAssetInfo':
      return setAssetInfo();
    case 'streamerSubscribe':
      return streamerSubscribe();
    case 'thanksReport':
      return thanksReport();
    case 'titleRequest':
      return titleRequest();
    case 'validEmail':
      return validateEmail();
    default:
      console.error('Unknown bits function', args.fn);
  }
}

function appeal() {
  if ($('.nav-tree').length) location.hash = location.hash || '#help-root';
  $('select.appeal-presets').on('change', function (this: HTMLSelectElement, e: Event) {
    $(this)
      .parents('form')
      .find('textarea')
      .val((e.target as HTMLTextAreaElement).value);
  });

  $('form.appeal__actions__slack').on('submit', (e: Event) => {
    const form = e.target as HTMLFormElement;
    formToXhr(form);
    $(form).find('button').text('Sent!').attr('disabled', 'true');
    return false;
  });
}

function autoForm({ selector, ops }: { selector: string; ops: string }) {
  const el = document.querySelector(selector) as HTMLElement;
  const oplist = ops.split(' ');
  if (!el || oplist.length === 0) return;
  if (oplist.includes('focus')) el.focus();
  if (oplist.includes('begin')) (el as HTMLInputElement).setSelectionRange(0, 0);
}

function colorizeYesNoTable() {
  document.querySelectorAll('.slist td').forEach((td: HTMLElement) => {
    if (td.textContent === 'YES') td.style.color = 'green';
    else if (td.textContent === 'NO') td.style.color = 'red';
  });
}

function contact() {
  location.hash ||= '#help-root';
  contactEmail();
}

export function contactEmail(): void {
  $('a.contact-email-obfuscated').one('click', function (this: HTMLLinkElement) {
    $(this).html('...');
    setTimeout(() => {
      const address = atob(this.dataset.email!);
      $(this).html(address).attr('href', `mailto:${address}`);
    }, 300);
    return false;
  });
}

function dailyFeed() {
  $('.emoji-details').each(function (this: HTMLElement) {
    flairPickerLoader(this);
  });
}

function embedReasonToggle() {
  const el = document.getElementById('form3-reason') as HTMLSelectElement;
  el.addEventListener('change', () => {
    $('.report-reason')
      .addClass('none')
      .filter('.report-reason-' + el.value)
      .removeClass('none');
  });
}

function eventCountdown() {
  $('.event .countdown').each(function () {
    if (!this.dataset.seconds) return;

    const $el = $(this);
    const seconds = parseInt(this.dataset.seconds) - 1;
    const target = new Date().getTime() + seconds * 1000;

    const second = 1000,
      minute = second * 60,
      hour = minute * 60,
      day = hour * 24;

    const redraw = function () {
      const distance = target - new Date().getTime();

      if (distance > 0) {
        $el.find('.days').text(Math.floor(distance / day).toString());
        $el.find('.hours').text(Math.floor((distance % day) / hour).toString());
        $el.find('.minutes').text(Math.floor((distance % hour) / minute).toString());
        $el.find('.seconds').text(
          Math.floor((distance % minute) / second)
            .toString()
            .padStart(2, '0'),
        );
      } else {
        clearInterval(interval);
        site.reload();
      }
    };
    const interval = setInterval(redraw, second);

    redraw();
  });
}

function hcaptcha() {
  const script = document.createElement('script');
  script.src = 'https://hcaptcha.com/1/api.js';

  if ('credentialless' in window && window.crossOriginIsolated) {
    const documentCreateElement = document.createElement;
    script.src = 'https://hcaptcha.com/1/api.js?onload=initHcaptcha&recaptchacompat=off';
    script.onload = () => {
      document.createElement = function () {
        const element = documentCreateElement.apply(this, arguments as any);
        if (element instanceof HTMLIFrameElement) element.setAttribute('credentialless', '');
        return element;
      };
    };
    (window as any).initHcaptcha = () => (document.createElement = documentCreateElement);
  }

  document.head.appendChild(script);
}

function importer() {
  const $form: Cash = $('main.importer form');

  $form.on('submit', () => setTimeout(() => $form.html(spinnerHtml), 50));

  $form.find('input[type=file]').on('change', function (this: HTMLInputElement) {
    const file = this.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = e => $form.find('textarea').val(e.target?.result as string);
    reader.readAsText(file);
  });
}

function pmAll() {
  $('.copy-url-button').on('click', function (e) {
    $('#form3-message').val($('#form3-message').val() + e.target.dataset.copyurl + '\n');
  });
}

function relayForm() {
  wireCropDialog({
    aspectRatio: 2 / 1,
    post: { url: $('.relay-image-edit').attr('data-post-url')!, field: 'image' },
    selectClicks: $('.select-image, .drop-target'),
    selectDrags: $('.drop-target'),
  });

  const $source = $('#form3-syncSource'),
    showSource = () =>
      $('.relay-form__sync').each(function (this: HTMLElement) {
        this.classList.toggle('none', !this.classList.contains(`relay-form__sync-${$source.val()}`));
      });

  $source.on('change', showSource);
  showSource();
}

function setAssetInfo() {
  $('#asset-version-date').text(site.info.date);
  $('#asset-version-commit')
    .attr('href', 'https://github.com/lichess-org/lila/commits/' + site.info.commit)
    .find('pre')
    .text(site.info.commit.slice(0, 7));
  $('#asset-version-upcoming')
    .attr('href', 'https://github.com/lichess-org/lila/compare/' + site.info.commit + '...master')
    .find('pre')
    .text('...');
  $('#asset-version-message').text(site.info.message);
}

function streamerSubscribe() {
  $('.streamer-show, .streamer-list').on('change', '.streamer-subscribe input', (e: Event) => {
    const target = e.target as HTMLInputElement;
    $(target)
      .parents('.streamer-subscribe')
      .each(function (this: HTMLElement) {
        text(
          $(this)
            .data('action')
            .replace(/set=[^&]+/, `set=${target.checked}`),
          { method: 'post' },
        );
      });
  });
}

function titleRequest() {
  $('.title-image-edit').each(function (this: HTMLElement) {
    wireCropDialog({
      post: { url: $(this).attr('data-post-url')!, field: 'image' },
      selectClicks: $(this).find('.drop-target'),
      selectDrags: $(this).find('.drop-target'),
    });
  });
}

function thanksReport() {
  const $button = $('button.report-block');
  $button.one('click', function () {
    $button.find('span').text('Blocking...');
    fetch($button.data('action')!, { method: 'post' }).then(async res =>
      $button.find('span').text(res.ok ? 'Blocked!' : 'Block error'),
    );
  });
}

function validateEmail() {
  const email = document.getElementById('new-email') as HTMLInputElement;
  const currentError = 'This is already your current email.';
  email.setCustomValidity(currentError);
  email.addEventListener('input', function () {
    email.setCustomValidity(email.validity.patternMismatch ? currentError : '');
  });
}
