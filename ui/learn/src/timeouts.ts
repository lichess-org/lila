const timeouts: number[] = [];

export function setTimeout<F extends () => void>(f: F, t: number) {
  timeouts.push(window.setTimeout(f, t));
}

export function clearTimeouts() {
  timeouts.forEach(t => clearTimeout(t));
  timeouts.length = 0;
}
