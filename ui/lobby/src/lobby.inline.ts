// these statements are wrapped in an iife to prevent scope pollution and injected into the html body
// after the DOM as an inline <script>. no imports or site globals are available here.

let cols = 0;

/* Move the timeline to/from the bottom depending on screen width. */

function layout() {
  const lobby = document.querySelector<HTMLElement>('main.lobby');
  if (!lobby) return;
  const newCols = Number(window.getComputedStyle(lobby).getPropertyValue('---cols'));
  if (newCols === cols) return;

  cols = newCols;
  document
    .querySelector<HTMLElement>(cols > 2 ? '.lobby__side' : '.lobby')
    ?.append(document.querySelector('.lobby__timeline') ?? '');
}

layout();
window.addEventListener('resize', layout);
