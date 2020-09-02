import widget from './widget';
import trans from './trans';
import pubsub from './pubsub';

// TODO doesn't need to be a widget.
// all interactions come from pubsub
export default function loadFriendsWidget() {
  widget("friends", (() => {
    const getId = function(titleName) {
      return titleName.toLowerCase().replace(/^\w+\s/, '');
    };
    const makeUser = function(titleName) {
      const split = titleName.split(' ');
      return {
        id: split[split.length - 1].toLowerCase(),
        name: split[split.length - 1],
        title: (split.length > 1) ? split[0] : undefined,
        playing: false,
        patron: false
      };
    };
    const renderUser = function(user) {
      const icon = '<i class="line' + (user.patron ? ' patron' : '') + '"></i>',
        titleTag = user.title ? ('<span class="utitle"' + (user.title === 'BOT' ? ' data-bot' : '') + '>' + user.title + '</span>&nbsp;') : '',
        url = '/@/' + user.name,
        tvButton = user.playing ? '<a data-icon="1" class="tv ulpt" data-pt-pos="nw" href="' + url + '/tv" data-href="' + url + '"></a>' : '';
      return '<div><a class="user-link ulpt" data-pt-pos="nw" href="' + url + '">' + icon + titleTag + user.name + '</a>' + tvButton + '</div>';
    };
    return {
      _create: function() {
        const self: any = this,
          el = self.element;

        self.$friendBoxTitle = el.find('.friend_box_title').click(function() {
          el.find('.content_wrap').toggleNone();
          if (!self.loaded) {
            self.loaded = true;
            window.lichess.socket.send('following_onlines');
          }
        });

        self.$nobody = el.find(".nobody");

        const data = {
          users: [],
          playing: [],
          patrons: [],
          ...el.data('preload')
        };
        self.trans = trans(data.i18n);
        self.set(data);

        pubsub.on('socket.in.following_onlines', d => {
          d.users = d.d;
          self.set(d);
        });
        ['enters', 'leaves', 'playing', 'stopped_playing'].forEach(k =>
          pubsub.on('socket.in.following_' + k, self[k])
        );
      },
      repaint: function() {
        const self: any = this;
        if (self.loaded) requestAnimationFrame(function() {
          const users = self.users,
            ids = Object.keys(users).sort();
          self.$friendBoxTitle.html(self.trans.vdomPlural('nbFriendsOnline', ids.length, self.loaded ? $('<strong>').text(ids.length) : '-'));
          self.$nobody.toggleNone(!ids.length);
          self.element.find('.list').html(
            ids.map(function(id) {
              return renderUser(users[id]);
            }).join('')
          );
        }.bind(self));
      },
      insert: function(titleName) {
        const self: any = this;
        const id = getId(titleName);
        if (!self.users[id]) self.users[id] = makeUser(titleName);
        return self.users[id];
      },
      set: function(d) {
        const self: any = this;
        self.users = {};
        let i;
        for (i in d.users) this.insert(d.users[i]);
        for (i in d.playing) this.insert(d.playing[i]).playing = true;
        for (i in d.patrons) this.insert(d.patrons[i]).patron = true;
        this.repaint();
      },
      enters: function(d) {
        const user = this.insert(d.d);
        user.playing = d.playing;
        user.patron = d.patron;
        this.repaint();
      },
      leaves: function(titleName) {
        const self: any = this;
        delete self.users[getId(titleName)];
        this.repaint();
      },
      playing: function(titleName) {
        this.insert(titleName).playing = true;
        this.repaint();
      },
      stopped_playing: function(titleName) {
        this.insert(titleName).playing = false;
        this.repaint();
      }
    };
  })());
}
