site.load.then(() => {
  const form = document.querySelector('.search__form') as HTMLFormElement,
    $form = $(form),
    $usernames = $form.find('.usernames input'),
    $userRows = $form.find('.user-row'),
    $result = $('.search__result');

  function getUsernames() {
    const us: string[] = [];
    $usernames.each(function (this: HTMLInputElement) {
      const u = this.value.trim();
      if (u) us.push(u);
    });
    return us;
  }

  function userChoices(row: HTMLTableRowElement) {
    const isSelected = (
      row: HTMLTableRowElement,
      rowClassName: string,
      user: string,
      dataKey: string,
    ): boolean => {
      const player: string = $form.data(dataKey);
      return row.classList.contains(rowClassName) && !!player.length && user == player;
    };
    const usernames = getUsernames();
    const $select = $(row).find('select').html('<option value=""></option>');
    for (const user of usernames) {
      $select.append(
        $('<option>')
          .attr({
            value: user,
            ...(isSelected(row, 'winner', user, 'req-winner') ||
            isSelected(row, 'loser', user, 'req-loser') ||
            isSelected(row, 'whiteUser', user, 'req-white') ||
            isSelected(row, 'blackUser', user, 'req-black')
              ? { selected: '' }
              : {}),
          })
          .text(user),
      );
    }
    row.classList.toggle('none', !usernames.length);
  }

  function reloadUserChoices() {
    $userRows.each(function (this: HTMLTableRowElement) {
      userChoices(this);
    });
  }
  reloadUserChoices();
  $usernames.on('input paste', reloadUserChoices);

  const toggleAiLevel = function () {
    $form.find('.opponent select').each(function (this: HTMLSelectElement) {
      $form.find('.aiLevel').toggleClass('none', this.value != '1');
      $form.find('.opponentName').toggleClass('none', this.value == '1');
    });
  };
  toggleAiLevel();
  $form.find('.opponent select').on('change', toggleAiLevel);

  function serialize() {
    const params = new URLSearchParams();
    for (const [k, v] of new FormData(form).entries()) {
      if (v != '') params.set(k, v as string);
    }
    return params.toString();
  }

  const serialized = serialize();
  $result.find('a.permalink').each(function (this: HTMLAnchorElement) {
    this.href = this.href.split('?')[0] + '?' + serialized;
  });

  const updatePagerLink = () =>
    $result.find('.infinite-scroll .pager a').each(function (this: HTMLAnchorElement) {
      this.href += '&' + serialized;
    });
  updatePagerLink();
  site.pubsub.on('content-loaded', updatePagerLink);

  $form.on('submit', () => {
    $form
      .find('input,select')
      .filter(function (this: HTMLInputElement) {
        return !this.value;
      })
      .attr('disabled', 'disabled');
    $form.addClass('searching');
  });
});
