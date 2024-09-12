import StrongSocket from 'common/socket';

site.load.then(() => {
  site.socket = new StrongSocket(`/socket/v5`, false, { params: { flag: 'simul' } });
  site.pubsub.on('socket.in.reload', async() => {
    const rsp = await fetch('/simul/reload');
    const html = await rsp.text();
    $('.simul-list__content').html(html);
    site.pubsub.emit('content-loaded');
  });
});
