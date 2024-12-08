import { init, classModule, attributesModule } from 'snabbdom';
import view from './view';
import type { Recap } from './interfaces';
import { makeSwiper } from './swiper';

interface Opts {
  recap: Recap;
  user: LightUser;
}

export function initModule(opts: Opts): void {
  const patch = init([classModule, attributesModule]);
  document.querySelectorAll('#recap-swiper').forEach((el: HTMLElement) => {
    patch(el, view(opts.recap, opts.user));
    makeSwiper(opts.recap)(el);
  });
}
