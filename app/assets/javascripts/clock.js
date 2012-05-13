$.widget("lichess.clock", {
    _create: function() {
        var self = this;
        this.options.time = parseFloat(this.options.time) * 1000;
        $.extend(this.options, {
            duration: this.options.time,
            state: 'ready'
        });
        this.element.addClass('clock_enabled');
    },
destroy: function() {
    this.stop();
    $.Widget.prototype.destroy.apply(this);
},
start: function() {
    var self = this;
    self.options.state = 'running';
    self.element.addClass('running');
    var end_time = new Date().getTime() + self.options.time;
    self.options.interval = setInterval(function() {
        if (self.options.state == 'running') {
            var current_time = Math.round(end_time - new Date().getTime());
            if (current_time <= 0) {
                clearInterval(self.options.interval);
                current_time = 0;
            }

            self.options.time = current_time;
            self._show();

            //If the timer completed, fire the buzzer callback
            current_time == 0 && $.isFunction(self.options.buzzer) && self.options.buzzer(self.element);
        } else {
            clearInterval(self.options.interval);
        }
    },
    1000);
},

    setTime: function(time) {
        this.options.time = parseFloat(time) * 1000;
        this._show();
    },

    stop: function() {
        clearInterval(this.options.interval);
        this.options.state = 'stop';
        this.element.removeClass('running');
    },

    _show: function() {
        this.element.text(this._formatDate(new Date(this.options.time)));
    },

    _formatDate: function(date) {
        minutes = this._prefixInteger(date.getMinutes(), 2);
        seconds = this._prefixInteger(date.getSeconds(), 2);
        return minutes + ':' + seconds;
    },

    _prefixInteger: function (num, length) {
        return (num / Math.pow(10, length)).toFixed(length).substr(2);
    }
});
