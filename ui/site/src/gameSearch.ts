window.lichess.load.then(() => {

  const form = document.querySelector('.search__form') as HTMLFormElement,
    $form = $(form),
    $usernames = $form.find(".usernames input"),
    $userRows = $form.find(".user-row"),
    $result = $(".search__result");

  function getUsernames() {
    const us: string[] = [];
    $usernames.each(function(this: HTMLInputElement) {
      const u = this.value.trim();
      if (u) us.push(u);
    });
    return us;
  }

  function userChoices(row) {
    var options = ["<option value=''></option>"];
    var isSelected = function(row, rowClassName, user, dataKey) {
      var player = $form.data(dataKey);
      return (row.classList.contains(rowClassName) && player.length && user == player) ? "selected" : ""
    }
    getUsernames().forEach(function(user) {
      const option: string[] = [];
      option.push("<option value='" + user + "'");
      option.push(isSelected(row, "winner", user, 'req-winner'));
      option.push(isSelected(row, "loser", user, 'req-loser'));
      option.push(isSelected(row, "whiteUser", user, 'req-white'));
      option.push(isSelected(row, "blackUser", user, 'req-black'));
      option.push(">" + user + "</option>");
      options.push(option.join(""));
    });
    $(row).find('select').html(options.join(""));
    row.classList.toggle('none', options.length < 2);
  }

  function reloadUserChoices() {
    $userRows.each(function(this: HTMLTableRowElement) {
      userChoices(this);
    });
  }
  reloadUserChoices();
  $usernames.on("input paste", reloadUserChoices);

  var toggleAiLevel = function() {
    $form.find(".opponent select").each(function(this: HTMLSelectElement) {
      $form.find('.aiLevel').toggleClass('none', this.value != "1");
      $form.find('.opponentName').toggleClass('none', this.value == "1");
    });
  };
  toggleAiLevel();
  $form.find(".opponent select").on('change', toggleAiLevel);

  function serialize() {
    const params = new URLSearchParams();
    for (const [k, v] of new FormData(form).entries()) {
      if (v != '') params.set(k, v as string);
    }
    return params.toString();
  }

  const serialized = serialize();
  $result.find("a.permalink").each(function(this: HTMLAnchorElement) {
    this.href = this.href.split('?')[0] + "?" + serialized;
  });
  $result.find('.search__rows').each(function(this: HTMLTableRowElement) {
    const table = this, next = table.querySelector(".pager a") as HTMLAnchorElement | null;
    if (!next) return;
    next.href = next.href + "&" + serialized;
    /* $(table).infinitescroll({ */
    /*   navSelector: ".pager", */
    /*   nextSelector: $(next), */
    /*   itemSelector: ".search__rows .paginated", */
    /*   loading: { */
    /*     msgText: "", */
    /*     finishedMsg: "---" */
    /*   } */
    /* }, function() { */
    /*   $("#infscr-loading").remove(); */
    /*   window.lichess.contentLoaded(table); */
    /* }); */
  });

  $form.on('submit', () => {
    $form.find("input,select").filter(function(this: HTMLInputElement) { return !this.value; }).attr("disabled", "disabled");
    $form.addClass('searching');
  });
});
