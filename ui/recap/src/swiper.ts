import Swiper from 'swiper';
import { SwiperOptions } from 'swiper/types';
import { Pagination } from 'swiper/modules';
import 'swiper/css';
import 'swiper/css/pagination';
import { Recap } from './interfaces';
import { animateNumber } from './ui';

export const makeSwiper =
  (_recap: Recap) =>
  (element: HTMLElement): void => {
    const options: SwiperOptions = {
      modules: [Pagination],
      initialSlide: 0,
      direction: 'vertical',
      loop: false,
      cssMode: true,
      keyboard: { enabled: true },
      mousewheel: true,
      pagination: {
        el: '.swiper-pagination',
        clickable: true,
      },
    };
    const swiper = new Swiper(element, options);
    element.focus();
    swiper.on('slideChange', function () {
      setTimeout(
        () =>
          element
            .querySelectorAll('.swiper-slide-active .animated-number')
            .forEach((counter: HTMLElement) => {
              animateNumber(counter);
            }),
        200,
      );
    });
  };
