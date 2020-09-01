lichess.widget('watchers', {
  _create: function() {
    this.list = this.element.find(".list");
    this.number = this.element.find(".number");
    lichess.pubsub.on('socket.in.crowd', data => this.set(data.watchers || data));
    lichess.watchersData && this.set(lichess.watchersData);
  },
  set: function(data) {
    lichess.watchersData = data;
    if (!data || !data.nb) return this.element.addClass('none');
    if (this.number.length) this.number.text(data.nb);
    if (data.users) {
      const tags = data.users.map(u => {
        const split = u.split(' ');
        const id = split.length == 1 ? split[0] : split[1];
        return u ? '<a class="user-link ulpt ' + (klass || '') + '" href="/@/' + id + '">' + (limit ? u.substring(0, limit) : u) + '</a>' : 'Anonymous';
      });
      if (data.anons === 1) tags.push('Anonymous');
      else if (data.anons) tags.push('Anonymous (' + data.anons + ')');
      this.list.html(tags.join(', '));
    } else if (!this.number.length) this.list.html(data.nb + ' players in the chat');
    this.element.removeClass('none');
  }
});
