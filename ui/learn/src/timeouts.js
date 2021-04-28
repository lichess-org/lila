const timeouts = [];

export function setTimeout(f, t) {
  timeouts.push(setTimeout(f, t));
}

export function clearTimeouts() {
  timeouts.forEach(t => clearTimeout(t));
  timeouts.length = 0;
}
