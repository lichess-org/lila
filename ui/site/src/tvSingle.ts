import * as xhr from 'common/xhr';
import throttlePromiseDelay from 'common/throttle';

const setZen = throttlePromiseDelay(1000, zen =>
  xhr.text('/pref/zen', {
    method: 'post',
    body: xhr.form({ zen: zen ? 1 : 0 }),
  })
);

lichess.load.then(() => {
  lichess.pubsub.on('zen', () => {
    const zen = $('body').toggleClass('zen').hasClass('zen');
    window.dispatchEvent(new Event('resize'));
    setZen(zen);
  });

  $('body').addClass('playing'); // for zen
  window.Mousetrap.bind('z', () => lichess.pubsub.emit('zen'));
  // don't add the click listener to #zentog since one is already
  // added in boot.ts
});
