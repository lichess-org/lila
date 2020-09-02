import widget from './widget';
import pubsub from './pubsub';

export default function loadWatchersWidget() {
  let watchersData;

  widget('watchers', {
    _create: function() {
      this.list = this.element.find(".list");
      this.number = this.element.find(".number");
      pubsub.on('socket.in.crowd', data => this.set(data.watchers || data));
      watchersData && this.set(watchersData);
    },
    set: function(data) {
      watchersData = data;
      if (!data || !data.nb) return this.element.addClass('none');
      if (this.number.length) this.number.text(data.nb);
      if (data.users) {
        const tags = data.users.map(u =>
          u ? `<a class="user-link ulpt" href="/@/${u.includes(' ') ? u.split(' ')[1] : u}">${u}</a>` : 'Anonymous'
        );
        if (data.anons === 1) tags.push('Anonymous');
        else if (data.anons) tags.push('Anonymous (' + data.anons + ')');
        this.list.html(tags.join(', '));
      } else if (!this.number.length) this.list.html(data.nb + ' players in the chat');
      this.element.removeClass('none');
    }
  });
}
