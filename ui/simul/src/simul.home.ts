import StrongSocket from 'common/socket';
import { pubsub } from 'common/pubsub';

site.load.then(() => {
  site.socket = new StrongSocket(`/socket/v5`, false, { params: { flag: 'simul' } });
  pubsub.on('socket.in.reload', async() => {
    const rsp = await fetch('/simul/reload');
    const html = await rsp.text();
    $('.simul-list__content').html(html);
    pubsub.emit('content-loaded');
  });
});
