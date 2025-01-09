import { loadCompiledScript, loadCssPath } from 'common/assets';
import { initiatingHtml } from './util';

export function challengeApp(): typeof window.lishogi.challengeApp {
  let instance: any, booted: boolean;
  const $toggle = $('#challenge-toggle');
  $toggle.one('mouseover click', function () {
    load();
  });
  const load = function (data?) {
    if (booted) return;
    booted = true;
    $('#challenge-app').html(initiatingHtml);
    loadCssPath('challenge');
    loadCompiledScript('challenge').then(() => {
      instance = window.lishogi.modules.challenge!({
        data: data,
        show: function () {
          if (!$('#challenge-app').is(':visible')) $toggle.trigger('click');
        },
        setCount: function (nb) {
          $toggle.find('span').attr('data-count', nb);
        },
        pulse: function () {
          $toggle.addClass('pulse');
        },
      });
    });
  };
  return {
    update: function (data) {
      if (!instance) load(data);
      else instance.update(data);
    },
    open: function () {
      $toggle.trigger('click');
    },
  };
}
