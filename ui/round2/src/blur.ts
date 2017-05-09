// Register blur events to be sent as move metadata

let lastFocus = -1;
let focusCutoff = Date.now() + 10000;

export function init() {
  window.addEventListener('focus', () => lastFocus = Date.now());
}

export function get() {
  return lastFocus > focusCutoff;
};

export function onMove() {
  focusCutoff = Date.now() + 1000;
};
