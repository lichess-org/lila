var xhr = require('./xhr');
var m = require('mithril');
var asText = require('./view').text;

module.exports = function(env) {

  this.data;

  this.vm = {
    initiating: true,
    scrolling: false
  };

  var readAllStorage = lichess.storage.make('notify-read-all');

  readAllStorage.listen(function() {
    this.data.unread = 0;
    env.setCount(0);
    m.redraw();
  }.bind(this));

  this.update = function(data, incoming) {
    this.data = data;
    if (this.data.pager.currentPage === 1 && this.data.unread && env.isVisible()) {
      env.setNotified();
      this.data.unread = 0;
      readAllStorage.set(1); // tell other tabs
    }
    if (data.i18n) this.trans = lichess.trans(data.i18n);
    this.vm.initiating = false;
    this.vm.scrolling = false;
    env.setCount(this.data.unread);
    if (incoming) this.notifyNew();
    m.redraw();
  }.bind(this);

  this.notifyNew = function() {
    if (this.data.pager.currentPage !== 1) return;
    var notif = this.data.pager.currentPageResults.filter(function(n) {
      return !n.read;
    })[0];
    if (!notif) return;
    env.pulse();
    if (!lichess.quietMode) $.sound.newPM();
    var text = asText(notif);
    if (text) lichess.desktopNotification(text);
  }.bind(this);

  this.loadFirstPage = function() {
    xhr.load().then(this.update);
  }.bind(this);

  this.nextPage = function() {
    if (!this.data.pager.nextPage) return;
    this.vm.scrolling = true;
    xhr.load(this.data.pager.nextPage).then(this.update);
    m.redraw();
  }.bind(this);

  this.previousPage = function() {
    if (!this.data.pager.previousPage) return;
    this.vm.scrolling = true;
    xhr.load(this.data.pager.previousPage).then(this.update);
    m.redraw();
  }.bind(this);

  this.setVisible = function() {
    if (!this.data || this.data.pager.currentPage === 1) this.loadFirstPage();
  }.bind(this);

  if (env.data) this.update(env.data, env.incoming)
  else this.loadFirstPage();
};
