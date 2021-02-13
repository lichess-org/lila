// Register blur events to be sent as move metadata

let lastFocus = 0;
let focusCutoff = 0;

export function init(withBlur: boolean) {
  if (!withBlur) focusCutoff = Date.now() + 10000;
  window.addEventListener('focus', () => (lastFocus = Date.now()));
}

export function get() {
  return lastFocus >= focusCutoff;
}

export function onMove() {
  focusCutoff = Date.now() + 1000;
}
