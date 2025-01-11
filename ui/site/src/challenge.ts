import { loadCompiledScript, loadCssPath } from 'common/assets';
import { initiatingHtml } from './util';

export function challengeApp(): typeof window.lishogi.challengeApp {
  let instance: any;
  let booted: boolean;
  const $toggle = $('#challenge-toggle');
  $toggle.one('mouseover click', () => {
    load();
  });
  const load = (data?) => {
    if (booted) return;
    booted = true;
    $('#challenge-app').html(initiatingHtml);
    loadCssPath('challenge');
    loadCompiledScript('challenge').then(() => {
      instance = window.lishogi.modules.challenge!({
        data: data,
        show: () => {
          if (!$('#challenge-app').is(':visible')) $toggle.trigger('click');
        },
        setCount: nb => {
          $toggle.find('span').attr('data-count', nb);
        },
        pulse: () => {
          $toggle.addClass('pulse');
        },
      });
    });
  };
  return {
    update: data => {
      if (!instance) load(data);
      else instance.update(data);
    },
    open: () => {
      $toggle.trigger('click');
    },
  };
}
