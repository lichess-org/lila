import { loadFlairPicker } from './flairPicker';

$('.emoji-details').each(function (this: HTMLElement) {
  loadFlairPicker(this);
});
