import { ResizeObserver } from '@juggle/resize-observer';
// See Chart.js issue #8414
export default function initModule() {
  window.ResizeObserver = ResizeObserver;
}
