lichess.widget = (name, prototype) => {
  const constructor = $[name] = function(options, element) {
    this.element = $(element);
    $.data(element, name, this);
    this.options = options;
    this._create();
  };
  constructor.prototype = prototype;
  $.fn[name] = function(method) {
    const args = Array.prototype.slice.call(arguments, 1);
    if (typeof method === 'string') this.each(function() {
      const instance = $.data(this, name);
      if (instance && $.isFunction(instance[method])) instance[method].apply(instance, args);
    });
    else this.each(function() {
      if (!$.data(this, name)) $.data(this, name, new constructor(method, this));
    });
    return this;
  };
};
