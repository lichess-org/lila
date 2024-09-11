import StrongSocket from 'common/socket';

site.load.then(async() => {
  site.socket = new StrongSocket(`/socket/v5`, false, { params: { flag: 'simul' } });
  const rsp = await fetch('/simul/reload');
  const html = await rsp.text();
  $('.simul-list__content').html(html);
  site.pubsub.emit('content-loaded');
});
