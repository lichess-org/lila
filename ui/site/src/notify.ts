import { loadCompiledScript, loadCssPath } from 'common/assets';
import { initiatingHtml } from './util';

export function notifyApp(): typeof window.lishogi.notifyApp {
  let instance: any, booted: boolean;
  const $toggle = $('#notify-toggle'),
    isVisible = () => $('#notify-app').is(':visible'),
    permissionChanged = () => {
      console.log('permissionChanged', instance);
      $toggle
        .find('span')
        .attr(
          'data-icon',
          'Notification' in window && Notification.permission == 'granted' ? '\ue00f' : '\xbf'
        );

      if (instance) instance.redraw();
    };

  if ('permissions' in navigator)
    navigator.permissions.query({ name: 'notifications' }).then(perm => {
      perm.onchange = permissionChanged;
    });

  permissionChanged();

  const load = (data?, incoming?) => {
    if (booted) return;
    booted = true;
    $('#notify-app').html(initiatingHtml);
    loadCssPath('notify');
    loadCompiledScript('notify').then(() => {
      instance = window.lishogi.modules.notify!({
        data: data,
        incoming: incoming,
        isVisible: isVisible,
        setCount(nb: number) {
          $toggle.find('span').attr('data-count', nb);
        },
        show() {
          if (!isVisible()) $toggle.click();
        },
        setNotified() {
          window.lishogi.socket.send('notified');
        },
        pulse() {
          $toggle.addClass('pulse');
        },
      });
    });
  };

  $toggle
    .one('mouseover click', () => load())
    .click(() => {
      if ('Notification' in window) Notification.requestPermission(() => permissionChanged());
      setTimeout(() => {
        if (instance && isVisible()) instance.setVisible();
      }, 200);
    });

  return {
    update(data: any, incoming: boolean) {
      if (!instance) load(data, incoming);
      else instance.update(data, incoming);
    },
    setMsgRead(user: string) {
      if (!instance) load();
      else instance.setMsgRead(user);
    },
  };
}
