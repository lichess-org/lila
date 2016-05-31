var xhr = require('./xhr');
var m = require('mithril');

module.exports = function(env) {

  this.pager;

  this.vm = {
    initiating: true,
    reloading: false
  };

  this.setPager = function(p) {
    this.vm.initiating = false;
    this.vm.reloading = false;
    this.pager = p;

    m.redraw();
  }.bind(this);

  this.updatePager = function() {
    this.vm.reloading = true;
    return xhr.load().then(this.setPager);
  }.bind(this);

  this.updateAndMarkAsRead = function() {
    this.vm.reloading = true;
    return xhr.markAllRead().then(function(p) {
      this.setPager(p);
      env.setCount(0);
    });
  }.bind(this);

  this.nextPage = function() {
    if (!this.pager.nextPage) return;
    this.vm.reloading = true;
    xhr.load(this.pager.nextPage).then(this.setPager);
  }.bind(this);

  this.previousPage = function() {
    if (!this.pager.previousPage) return;
    this.vm.reloading = true;
    xhr.load(this.pager.previousPage).then(this.setPager);
  }.bind(this);

  this.updatePager();
};
