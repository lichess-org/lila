(function() {
  const bowser = require('bowser');
  const browser = bowser.getParser(window.navigator.userAgent);

  if (browser.getPlatformType() === 'desktop' && !supportBrowser(browser)) {
    $('#unsupported_browser a').click(function(e) {
      e.preventDefault();
      $('#unsupported_browser').hide();
    });
    $('#unsupported_browser').show();
  }

  function supportBrowser(browser) {
    return browser.satisfies({
      chrome: '>44',
      firefox: '>43',
      opera: ">34",
      safari: ">9",
      'Internet Explorer': '>10.99',
      edge: '>12'
    });
  }
})();
