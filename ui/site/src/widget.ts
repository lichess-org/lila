// @ts-nocheck

import { i18nVdomPlural } from 'i18n';

const widget = (name: string, prototype: any): any => {
  const constructor = ($[name] = function (options, element) {
    this.element = $(element);
    $.data(element, name, this);
    this.options = options;
    this._create();
  });
  constructor.prototype = prototype;
  $.fn[name] = function (method) {
    const returnValue = this;
    const args = Array.prototype.slice.call(arguments, 1);
    if (typeof method === 'string')
      this.each(function () {
        const instance = $.data(this, name);
        if (!instance) return;
        if (!$.isFunction(instance[method]) || method.charAt(0) === '_')
          return $.error("no such method '" + method + "' for " + name + ' widget instance');
        returnValue = instance[method].apply(instance, args);
      });
    else
      this.each(function () {
        if (!$.data(this, name)) $.data(this, name, new constructor(method, this));
      });
    return returnValue;
  };
};

interface WatchersData {
  users?: string[];
  nb?: number;
  anons?: number;
}

export function initWidgets(): void {
  let watchersData: WatchersData;
  widget('watchers', {
    _create: function () {
      this.list = this.element.find('.list');
      this.number = this.element.find('.number');
      window.lishogi.pubsub.on('socket.in.crowd', data => this.set(data.watchers || data));
      watchersData && this.set(watchersData);
    },
    set: function (data: WatchersData) {
      watchersData = data;
      if (!data || !data.nb) return this.element.addClass('none');
      if (this.number.length) this.number.text(data.nb);
      if (data.users) {
        const tags = data.users.map(u =>
          u ? `<a class="user-link ulpt" href="/@/${u.toLowerCase()}">${u}</a>` : 'Anonymous',
        );
        if (data.anons === 1) tags.push('Anonymous');
        else if (data.anons) tags.push('Anonymous (' + data.anons + ')');
        this.list.html(tags.join(', '));
      } else if (!this.number.length) this.list.html(data.nb + ' players in the chat');
      this.element.removeClass('none');
    },
  });

  widget(
    'friends',
    (() => {
      const getId = (titleName: string) => titleName.toLowerCase().replace(/^\w+\s/, '');
      const makeUser = (titleName: string) => {
        const split = titleName.split(' ');
        return {
          id: split[split.length - 1].toLowerCase(),
          name: split[split.length - 1],
          title: split.length > 1 ? split[0] : undefined,
          playing: false,
          patron: false,
        };
      };
      const renderUser = (user: any) => {
        const icon = '<i class="line' + (user.patron ? ' patron' : '') + '"></i>',
          titleTag = user.title
            ? '<span class="title"' +
              (user.title === 'BOT' ? ' data-bot' : '') +
              '>' +
              user.title +
              '</span>&nbsp;'
            : '',
          url = '/@/' + user.name,
          tvButton = user.playing
            ? '<a data-icon="1" class="tv ulpt" data-pt-pos="nw" href="' +
              url +
              '/tv" data-href="' +
              url +
              '"></a>'
            : '';
        return (
          '<div><a class="user-link ulpt" data-pt-pos="nw" href="' +
          url +
          '">' +
          icon +
          titleTag +
          user.name +
          '</a>' +
          tvButton +
          '</div>'
        );
      };
      return {
        _create: function () {
          const el = this.element;

          this.$friendBoxTitle = el.find('.friend_box_title').on('click', () => {
            el.find('.content_wrap').toggleNone();
            if (!this.loaded) {
              this.loaded = true;
              window.lishogi.socket.send('following_onlines');
            }
          });

          this.$nobody = el.find('.nobody');

          const data = {
            users: [],
            playing: [],
            patrons: [],
            ...el.data('preload'),
          };
          this.set(data);
        },
        repaint: function () {
          if (this.loaded)
            requestAnimationFrame(
              function () {
                const users = this.users,
                  ids = Object.keys(users).sort();
                this.$friendBoxTitle.html(
                  i18nVdomPlural(
                    'nbFriendsOnline',
                    ids.length,
                    this.loaded ? $('<strong>').text(ids.length) : '-',
                  ),
                );
                this.$nobody.toggleNone(!ids.length);
                this.element.find('.list').html(ids.map(id => renderUser(users[id])).join(''));
              }.bind(this),
            );
        },
        insert: function (titleName) {
          const id = getId(titleName);
          if (!this.users[id]) this.users[id] = makeUser(titleName);
          return this.users[id];
        },
        set: function (d) {
          this.users = {};
          let i;
          for (i in d.users) this.insert(d.users[i]);
          for (i in d.playing) this.insert(d.playing[i]).playing = true;
          for (i in d.patrons) this.insert(d.patrons[i]).patron = true;
          this.repaint();
        },
        enters: function (d) {
          const user = this.insert(d.d);
          user.playing = d.playing;
          user.patron = d.patron;
          this.repaint();
        },
        leaves: function (titleName) {
          delete this.users[getId(titleName)];
          this.repaint();
        },
        playing: function (titleName) {
          this.insert(titleName).playing = true;
          this.repaint();
        },
        stopped_playing: function (titleName) {
          this.insert(titleName).playing = false;
          this.repaint();
        },
      };
    })(),
  );

  widget('clock', {
    _create: function () {
      const target = this.options.time * 1000 + Date.now();
      const timeEl = this.element.find('.time')[0];
      const tick = () => {
        const remaining = target - Date.now();
        if (remaining <= 0) clearInterval(this.interval);
        timeEl.innerHTML = this._formatMs(remaining);
      };
      this.interval = setInterval(tick, 1000);
      tick();
    },

    _pad: x => (x < 10 ? '0' : '') + x,

    _formatMs: function (msTime) {
      const date = new Date(Math.max(0, msTime + 500));

      const hours = date.getUTCHours(),
        minutes = date.getUTCMinutes(),
        seconds = date.getUTCSeconds();

      if (hours > 0) {
        return hours + ':' + this._pad(minutes) + ':' + this._pad(seconds);
      } else {
        return minutes + ':' + this._pad(seconds);
      }
    },
  });
}
