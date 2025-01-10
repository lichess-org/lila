import { spinnerHtml } from 'common/spinner';

// Unique id for the current document/navigation. Should be different after
// each page load and for each tab. Should be unpredictable and secret while used
export function generateSri(): string {
  try {
    const data = window.crypto.getRandomValues(new Uint8Array(9));
    return btoa(String.fromCharCode(...data)).replace(/[/+]/g, '_');
  } catch (_) {
    return Math.random().toString(36).slice(2, 12);
  }
}

export const initiatingHtml: string = `<div class="initiating">${spinnerHtml}</div>`;
