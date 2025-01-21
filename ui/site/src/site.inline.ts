// these statements are wrapped in an iife and injected into the html body
// after the DOM as an inline <script>. no imports allowed here.

if (!window.site) window.site = {} as Site;
if (!window.site.load)
  window.site.load = new Promise<void>(function (resolve) {
    document.addEventListener('DOMContentLoaded', function () {
      resolve();
    });
  });
