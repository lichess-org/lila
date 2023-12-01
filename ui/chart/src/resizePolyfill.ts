import { ResizeObserver } from '@juggle/resize-observer';
// See Chart.js issue #8414
export default async function initModule() {
  window.ResizeObserver = ResizeObserver;
}
