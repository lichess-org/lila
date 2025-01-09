window.lishogi.ready.then(() => {
  const $form = $('.search__form');
  const $usernames = $form.find('.usernames input');
  const $userRows = $form.find('.user-row');

  function getUsernames() {
    const us: string[] = [];
    $usernames.each(function () {
      const u = $(this).val().trim();
      if (u) us.push(u);
    });
    return us;
  }

  function userChoices(row) {
    const options = ["<option value=''></option>"],
      isSelected = function (row, rowClassName, user, dataKey): string {
        const player = $form.data(dataKey);
        return row.classList.contains(rowClassName) && player.length && user == player
          ? 'selected'
          : '';
      };
    getUsernames().forEach(function (user) {
      const option: string[] = [];
      option.push("<option value='" + user + "'");
      option.push(isSelected(row, 'winner', user, 'req-winner'));
      option.push(isSelected(row, 'loser', user, 'req-loser'));
      option.push(isSelected(row, 'senteUser', user, 'req-sente'));
      option.push(isSelected(row, 'goteUser', user, 'req-gote'));
      option.push('>' + user + '</option>');
      options.push(option.join(''));
    });
    $(row).find('select').html(options.join(''));
    $(row).toggleNone(options.length > 1);
  }

  function reloadUserChoices() {
    $userRows.each(function () {
      userChoices(this);
    });
  }
  reloadUserChoices();
  $usernames.on('input paste', reloadUserChoices);

  const toggleAiLevel = function () {
    $form.find('.opponent select').each(function () {
      $form.find('.aiLevel').toggleNone($(this).val() == 1);
      $form.find('.opponentName').toggleNone($(this).val() != 1);
    });
  };
  toggleAiLevel();
  $form.find('.opponent select').change(toggleAiLevel);

  const serialize = function (all = false) {
    const sel = $form.find('input,select');
    return (all ? sel : sel.not('[type=hidden]'))
      .filter(function () {
        return !!(this as HTMLInputElement).value;
      })
      .serialize();
  };

  const serialized = serialize();

  const result = document.querySelector('')!,
    rows = result.querySelectorAll('.search__rows');

  result.querySelectorAll('a.permalink').forEach(el => {
    const s = el.classList.contains('download') ? serialize(true) : serialized;
    el.setAttribute('href', (el.getAttribute('href') || '').split('?')[0] + '?' + s);
  });

  rows.forEach(row => {
    const nextLink = row.querySelector<HTMLAnchorElement>('.pager a');
    if (!nextLink) return;

    const currentHref = nextLink.href;
    nextLink.href = currentHref + (currentHref.includes('?') ? '&' : '?') + serialized;

    const infScroll = new window.InfiniteScroll(row, {
      path: '.pager a',
      append: '.search__rows .paginated',
      history: false,
      hideNav: '.pager',
    });

    infScroll.on('append', () => {
      window.lishogi.pubsub.emit('content_loaded');
    });
  });

  $form.submit(function () {
    $form
      .find('input,select')
      .filter(function () {
        return !(this as HTMLInputElement).value;
      })
      .attr('disabled', 'disabled');
    $form.addClass('searching');
  });
});
