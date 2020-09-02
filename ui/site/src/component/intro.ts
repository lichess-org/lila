/// <reference types="../../types/info" />

console.info("Lichess is open source! https://lichess.org/source");

export const info = __info__;

export const hasTouchEvents = 'ontouchstart' in window;
export const spinnerHtml = '<div class="spinner"><svg viewBox="0 0 40 40"><circle cx=20 cy=20 r=18 fill="none"></circle></svg></div>';

// Unique id for the current document/navigation. Should be different after
// each page load and for each tab. Should be unpredictable and secret while
// in use.
let _sri: string;
try {
  const data = window.crypto.getRandomValues(new Uint8Array(9));
  _sri = btoa(String.fromCharCode(...data)).replace(/[/+]/g, '_');
} catch (_) {
  _sri = Math.random().toString(36).slice(2, 12);
}

export const sri = _sri;
