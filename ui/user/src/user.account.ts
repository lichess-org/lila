import * as licon from 'lib/licon';
import * as xhr from 'lib/xhr';
import { storage } from 'lib/storage';
import { addPasswordVisibilityToggleListener } from 'lib/view/controls';
import flairPickerLoader from 'bits/flairPicker';
import { confirm } from 'lib/view/dialogs';

site.load.then(() => {
  $('.emoji-details').each(function (this: HTMLElement) {
    flairPickerLoader(this);
  });

  addPasswordVisibilityToggleListener();

  const localPrefs: [string, string, string, boolean][] = [
    ['behavior', 'arrowSnap', 'arrow.snap', true],
    ['behavior', 'scrollMoves', 'scrollMoves', true],
    ['notification', 'playBellSound', 'playBellSound', true],
  ];

  $('.security table form').on('submit', function (this: HTMLFormElement) {
    xhr.text(this.action, { method: 'post', body: new URLSearchParams(new FormData(this) as any) });
    $(this).parent().parent().remove();
    return false;
  });

  $('form.autosubmit').each(function (this: HTMLFormElement) {
    const form = this,
      $form = $(form),
      showSaved = () => $form.find('.saved').removeClass('none');
    computeBitChoices($form, 'behavior.submitMove');
    $form.find('input').on('change', function (this: HTMLInputElement) {
      computeBitChoices($form, 'behavior.submitMove');
      localPrefs.forEach(([categ, name, storeKey]) => {
        if (this.name === `${categ}.${name}`) {
          storage.boolean(storeKey).set(this.value === '1');
          showSaved();
        }
      });
      xhr.formToXhr(form).then(() => {
        showSaved();
        storage.fire('reload-round-tabs');
      });
    });
  });

  localPrefs.forEach(([categ, name, storeKey, def]) =>
    $(`#ir${categ}_${name}_${storage.boolean(storeKey).getOrDefault(def) ? 1 : 0}`).prop('checked', true),
  );

  $('form[action="/account/oauth/token/create"]').each(function (this: HTMLFormElement) {
    const form = $(this),
      submit = form.find('button.submit');
    let isDanger = false;
    const checkDanger = () => {
      isDanger = !!form.find('.danger input:checked').length;
      submit.toggleClass('button-red', isDanger);
      submit.attr('data-icon', isDanger ? licon.CautionTriangle : licon.Checkmark);
      submit.attr('title', isDanger ? submit.data('danger-title') : '');
    };
    checkDanger();
    form.find('input').on('change', checkDanger);
    submit.on('click', function (this: HTMLElement, e: Event) {
      if (!isDanger) return true;
      e.preventDefault();
      confirm(this.title).then(yes => {
        if (yes) (form[0] as HTMLFormElement).submit();
      });
      return false;
    });
  });

  $('form.dirty-alert').each(function (this: HTMLFormElement) {
    const form = this;
    const serialize = () => {
      const data = new FormData(form);
      return Array.from(data.keys())
        .map(k => `${k}=${data.get(k)}`)
        .join('&');
    };
    let clean = serialize();
    $(form).on('submit', () => {
      clean = serialize();
    });
    window.addEventListener('beforeunload', e => {
      if (
        clean !== serialize() &&
        !window.confirm('You have unsaved changes. Are you sure you want to leave?')
      )
        e.preventDefault();
    });
  });
});

function computeBitChoices($form: Cash, name: string) {
  let sum = 0;
  $form.find(`input[type="checkbox"][data-name="${name}"]:checked`).each(function (this: HTMLInputElement) {
    sum |= parseInt(this.value);
  });
  $form.find(`input[type="hidden"][name="${name}"]`).val(sum.toString());
}
