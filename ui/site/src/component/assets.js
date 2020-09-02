{
  lichess.assetUrl = (path, opts) => {
    opts = opts || {};
    const baseUrl = opts.sameDomain ? '' : document.body.getAttribute('data-asset-url'),
      version = opts.version || document.body.getAttribute('data-asset-version');
    return baseUrl + '/assets' + (opts.noVersion ? '' : '/_' + version) + '/' + path;
  };

  lichess.soundUrl = lichess.assetUrl('sound', {
    version: '000003'
  });

  const loadedCss = new Map();

  lichess.loadCss = url => {
    if (!loadedCss.has(url)) {
      loadedCss.set(url, true);
      $('head').append($('<link rel="stylesheet" type="text/css" />').attr('href', lichess.assetUrl(url)));
    }
  };

  lichess.loadCssPath = key =>
    lichess.loadCss(`css/${key}.${$('body').data('theme')}.${$('body').data('dev') ? 'dev' : 'min'}.css`);

  lichess.jsModule = name =>
    `compiled/lichess.${name}${$('body').data('dev') ? '' : '.min'}.js`;

  lichess.loadScript = (url, opts) =>
    $.ajax({
      dataType: "script",
      cache: true,
      url: lichess.assetUrl(url, opts)
    });

  lichess.hopscotch = f => {
    lichess.loadCss('vendor/hopscotch/dist/css/hopscotch.min.css');
    lichess.loadScript('vendor/hopscotch/dist/js/hopscotch.min.js', {
      noVersion: true
    }).done(f);
  }

  lichess.slider = () =>
    lichess.loadScript(
      'javascripts/vendor/jquery-ui.slider' + (lichess.hasTouchEvents ? '.touch' : '') + '.min.js'
    );
}
