function base64(base64String) {
  const padding = '='.repeat((4 - base64String.length % 4) % 4);
  const base64 = (base64String + padding)
    .replace(/\-/g, '+')
    .replace(/_/g, '/');

  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);

  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}

$(function() {
  const publicKey = base64('BO7W5UeO4UMvWtQmKRVFODWNmxj1or2aqPzYo8J3wRM9FgJIvpwOjWfn2B3I_JVhR3ciH-ZDx13be5cMNHYVCns');
  const privateKey = 'Sp21Z0befCFo4yOK4Lavl2HQwvKoft8YlDkWbAVygW4';

  const $btn = $('#subscribed');

  navigator.serviceWorker.ready.then(serviceWorker => {
    serviceWorker.pushManager.getSubscription().then(function(subscription) {
      console.log('status:', JSON.stringify(subscription));
      $btn
        .prop('checked', subscription !== null)
        .attr('disabled', false)
        .click(function() {
          $btn.attr('disabled', true);
          if ($btn.is(':checked')) {
            serviceWorker.pushManager.subscribe({
              userVisibleOnly: true,
              applicationServerKey: publicKey
            }).then(sub => {
              subscription = sub;
              fetch('/push/subscribe', {
                method: 'POST',
                headers: {
                  'Content-Type': 'application/json'
                },
                body: JSON.stringify(sub)
              });
              console.log(JSON.stringify(subscription));
              $btn.attr('disabled', false);
            }).catch(err => {
              console.error('failed to subscribe', err.message);
              $btn.attr('disabled', false).prop('checked', false);
            });
          } else {
            subscription.unsubscribe().then(() => {
              subscription = null;
              console.log('Unsubscribed!');
              $btn.attr('disabled', false);
            }).catch(err => {
              console.error('failed to subscribe', err);
              $btn.attr('disabled', false).prop('checked', true);
            });
          }
        });
    });
  });
});
