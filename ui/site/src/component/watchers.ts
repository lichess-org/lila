interface Data {
  nb: number;
  users?: string[];
  anons?: number;
  watchers?: Data;
}

let watchersData: Data | undefined;

export default function watchers(element: HTMLElement) {
  const $element = $(element);

  if ($element.data('watched')) return;

  $element.data('watched', 1);
  const $innerElement = $('<div class="chat__members__inner">').appendTo($element);
  const $numberEl = $(
    '<div class="number-container" title="Spectators"><span data-icon="r"></span><span class="number"></span></div>'
  )
    .appendTo($innerElement)
    .find('.number');
  const $listEl = $('<div class="list">').appendTo($innerElement);

  lichess.pubsub.on('socket.in.crowd', data => set(data.watchers || data));

  const set = (data: Data) => {
    watchersData = data;

    if (!data || !data.nb) return $element.addClass('none');

    $numberEl.text('' + data.nb);

    if (data.users) {
      const tags = data.users.map(u =>
        u ? `<a class="user-link ulpt" href="/@/${u.includes(' ') ? u.split(' ')[1] : u}">${u}</a>` : 'Anonymous'
      );
      if (data.anons === 1) tags.push('Anonymous');
      else if (data.anons) tags.push(`Anonymous (${data.anons})`);
      $listEl.html(tags.join(', '));
    } else $listEl.html('');

    $element.removeClass('none');
  };

  if (watchersData) set(watchersData);
}
