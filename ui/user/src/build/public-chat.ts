window.lishogi.ready.then(() => {
  let autoRefreshEnabled = true;
  let autoRefreshOnHold = false;

  const renderButton = () => {
    $('#auto_refresh')
      .toggleClass('active', autoRefreshEnabled)
      .toggleClass('hold', autoRefreshOnHold);
  };

  const onPageReload = () => {
    $('#communication').append(
      $('<a id="auto_refresh" class="button">Auto refresh</a>').on('click', () => {
        autoRefreshEnabled = !autoRefreshEnabled;
        renderButton();
      }),
    );
    renderButton();

    $('#communication .chat').each(function () {
      this.scrollTop = 99999;
    });

    $('#communication')
      .on('mouseenter', '.chat', () => {
        autoRefreshOnHold = true;
        $('#auto_refresh').addClass('hold');
      })
      .on('mouseleave', '.chat', () => {
        autoRefreshOnHold = false;
        $('#auto_refresh').removeClass('hold');
      });
  };
  onPageReload();

  setInterval(() => {
    if (!autoRefreshEnabled || document.visibilityState === 'hidden' || autoRefreshOnHold) return;

    // Reload only the chat grid portions of the page
    $('#comm-wrap').load('/mod/public-chat #communication', onPageReload);
  }, 4000);
});
