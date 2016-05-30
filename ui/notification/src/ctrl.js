var xhr = require('./xhr');

module.exports = function(env) {

    this.data = [];

    this.vm = {
        initiating: true,
        reloading: false
    };

    this.setInitialNotifications = function(data) {
        this.vm.initiating = false;
        this.vm.reloading = false;
        this.data = data;
    }.bind(this);

    this.markAllReadServer = function () {
        xhr.markAllRead();
    }.bind(this);

    this.addNewNotification = function(newNotification) {
        this.data.unshift(newNotification);

        // We only show the most recent notifications - the user should click 'see more' if they want
        // to see older notifications
        if (this.data.length > env.maxNotifications) this.data.pop();

        m.redraw();
    }.bind(this);

    xhr.load().then(this.setInitialNotifications);
};
