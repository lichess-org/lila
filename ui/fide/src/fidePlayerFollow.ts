import { debounce } from 'lib/async';
import { text } from 'lib/xhr';
import { pubsub } from 'lib/pubsub';

export function initModule(): void {
  fidePlayerFollow();
  pubsub.on('content-loaded', fidePlayerFollow);
}

function fidePlayerFollow(el?: HTMLElement): void {
  (el || document.body)
    .querySelectorAll<HTMLInputElement>('.fide-player__follow input:not(.loaded)')
    .forEach(el => {
      el.addEventListener(
        'change',
        debounce(
          e =>
            text(
              $(e.target)
                .data('action')
                .replace(/follow=[^&]+/, `follow=${$(e.target).prop('checked')}`),
              { method: 'post' },
            ),
          1000,
          true,
        ),
      );
      el.classList.add('loaded');
    });
}
