// import * as xhr from './xhr'
import { Ctrl, ChallengeOpts, ChallengeData, ChallengeUser, Redraw } from './interfaces'

export default function(opts: ChallengeOpts, redraw: Redraw): Ctrl {

  let data: ChallengeData | undefined;

  let initiating = true
  let reloading = false
  let trans: Trans | undefined

  function update(d: ChallengeData) {
    data = d;
    if (d.i18n) trans = window.lichess.trans(d.i18n);
    initiating = false;
    reloading = false;
    opts.setCount(countActiveIn());
    notifyNew();
    redraw();
  }

  function countActiveIn() {
    return data ? data.in.filter(c => !c.declined).length : 0;
  }

  function notifyNew() {
    data && data.in.forEach(c => {
      if (window.lichess.once('c-' + c.id)) {
        if (!window.lichess.quietMode) {
          opts.show();
          window.lichess.sound.newChallenge();
        }
        c.challenger && window.lichess.desktopNotification(showUser(c.challenger) + ' challenges you!');
        opts.pulse();
      }
    });
  }

  function showUser(user: ChallengeUser) {
    var rating = user.rating + (user.provisional ? '?' : '');
    var fullName = (user.title ? user.title + ' ' : '') + user.name;
    return fullName + ' (' + rating + ')';
  }

  return {
    data: () => data,
    initiating: () => initiating,
    reloading: () => reloading,
    trans: trans,
    update: update
  };


  // var all = function() {
  //   return this.data.in ? this.data.in.concat(this.data.out) : [];
  // }.bind(this);


  // this.idsHash = function() {
  //   return all().map(function(c) {
  //     return c.id;
  //   }).join('');
  // }.bind(this);

  // this.update = function(data) {
  //   this.data = data;
  //   if (data.i18n) this.trans = lichess.trans(data.i18n);
  //   this.vm.initiating = false;
  //   this.vm.reloading = false;
  //   env.setCount(this.countActiveIn());
  //   this.notifyNew();
  //   m.redraw();
  // }.bind(this);

  // this.decline = function(id) {
  //   this.data.in.forEach(function(c) {
  //     if (c.id === id) {
  //       c.declined = true;
  //       xhr.decline(id);
  //     }
  //   }.bind(this));
  // }.bind(this);

  // this.cancel = function(id) {
  //   this.data.out.forEach(function(c) {
  //     if (c.id === id) {
  //       c.declined = true;
  //       xhr.cancel(id);
  //     }
  //   }.bind(this));
  // }.bind(this);

  // if (env.data) this.update(env.data)
  // else xhr.load().then(this.update);

  // this.trans = function(key) {
  //   return key;
  // };
};
