window.lishogi.ready.then(() => {
  const $button = $('button.report-block');
  $button.one('click', () => {
    $button.find('span').text('Blocking...');
    fetch($button.attr('action')!, { method: 'post' }).then(() =>
      $button.find('span').text('Blocked!'),
    );
  });
});
