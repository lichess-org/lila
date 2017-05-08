// Register blur events to be sent as move metadata

let lastFocus;
let lastMove;

export function init() {
  window.addEventListener('focus', () => lastFocus = Date.now());
}

export function get() {
  return lastFocus - lastMove > 1000;
};

export function onMove() {
  lastMove = Date.now();
};
