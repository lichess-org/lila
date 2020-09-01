lichess.widget("friends", (() => {
  var getId = function(titleName) {
    return titleName.toLowerCase().replace(/^\w+\s/, '');
  };
  var makeUser = function(titleName) {
    var split = titleName.split(' ');
    return {
      id: split[split.length - 1].toLowerCase(),
      name: split[split.length - 1],
      title: (split.length > 1) ? split[0] : undefined,
      playing: false,
      patron: false
    };
  };
  var renderUser = function(user) {
    const icon = '<i class="line' + (user.patron ? ' patron' : '') + '"></i>',
      titleTag = user.title ? ('<span class="utitle"' + (user.title === 'BOT' ? ' data-bot' : '') + '>' + user.title + '</span>&nbsp;') : '',
      url = '/@/' + user.name,
      tvButton = user.playing ? '<a data-icon="1" class="tv ulpt" data-pt-pos="nw" href="' + url + '/tv" data-href="' + url + '"></a>' : '';
    return '<div><a class="user-link ulpt" data-pt-pos="nw" href="' + url + '">' + icon + titleTag + user.name + '</a>' + tvButton + '</div>';
  };
  return {
    _create: function() {
      const self = this,
        el = self.element;

      self.$friendBoxTitle = el.find('.friend_box_title').click(function() {
        el.find('.content_wrap').toggleNone();
        if (!self.loaded) {
          self.loaded = true;
          lichess.socket.send('following_onlines');
        }
      });

      self.$nobody = el.find(".nobody");

      const data = {
        users: [],
        playing: [],
        patrons: [],
        ...el.data('preload')
      };
      self.trans = lichess.trans(data.i18n);
      self.set(data);
    },
    repaint: function() {
      if (this.loaded) requestAnimationFrame(function() {
        const users = this.users,
          ids = Object.keys(users).sort();
        this.$friendBoxTitle.html(this.trans.vdomPlural('nbFriendsOnline', ids.length, this.loaded ? $('<strong>').text(ids.length) : '-'));
        this.$nobody.toggleNone(!ids.length);
        this.element.find('.list').html(
          ids.map(function(id) {
            return renderUser(users[id]);
          }).join('')
        );
      }.bind(this));
    },
    insert: function(titleName) {
      const id = getId(titleName);
      if (!this.users[id]) this.users[id] = makeUser(titleName);
      return this.users[id];
    },
    set: function(d) {
      this.users = {};
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
      delete this.users[getId(titleName)];
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
