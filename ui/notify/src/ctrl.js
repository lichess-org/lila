var xhr = require('./xhr');
var m = require('mithril');

module.exports = function(env) {

  this.data;

  this.vm = {
    initiating: true
  };

  this.update = function(data) {
    this.data = data;
    if (this.data.pager.currentPage === 1 && this.data.unread && env.isVisible()) {
      env.setNotified();
      this.data.unread = 0;
    } else this.notifyNew();
    if (data.i18n) this.trans = lichess.trans(data.i18n);
    this.vm.initiating = false;
    env.setCount(this.data.unread);
    m.redraw();
  }.bind(this);

  this.notifyNew = function() {
    // if (this.data.unread)
    // this.data.pager.currentPageResults.forEach(function(n) {
    //   if (n.unread) {
    //     if (!lichess.quietMode) {
    //       env.show();
    //       $.sound.newPM();
    //     }
    //     lichess.desktopNotification("New notification! <more details here>");
    //   }
    // });
  }.bind(this);

  this.loadFirstPage = function() {
    xhr.load().then(this.update);
  }.bind(this);

  this.nextPage = function() {
    if (!this.data.pager.nextPage) return;
    xhr.load(this.data.pager.nextPage).then(this.update);
  }.bind(this);

  this.previousPage = function() {
    if (!this.data.pager.previousPage) return;
    xhr.load(this.data.pager.previousPage).then(this.update);
  }.bind(this);

  this.setVisible = function() {
    if (!this.data || this.data.pager.currentPage === 1) this.loadFirstPage();
  }.bind(this);

  if (env.data) this.update(env.data)
  else this.loadFirstPage();
};
