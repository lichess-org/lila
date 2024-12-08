import { init, classModule, attributesModule } from 'snabbdom';
import view from './view';
import type { Recap } from './interfaces';
import { makeSwiper } from './swiper';
import { json } from 'common/xhr';

interface Opts {
  recap: Recap;
  user: LightUser;
}

export function initModule(opts: Opts | undefined): void {
  if (opts) loadRecap(opts);
  else awaitRecap();
}

function loadRecap(opts: Opts) {
  const patch = init([classModule, attributesModule]);
  document.querySelectorAll('#recap-swiper').forEach((el: HTMLElement) => {
    patch(el, view(opts.recap, opts.user));
    makeSwiper(opts.recap)(el);
  });
}

async function awaitRecap() {
  const opts: Opts = await json(location.pathname);
  loadRecap(opts);
}
