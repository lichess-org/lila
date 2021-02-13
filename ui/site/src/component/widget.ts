const widget = (name: string, prototype: any) => {
  const constructor = $[name] = function(options, element) {
    const self: any = this;
    self.element = $(element);
    $.data(element, name, this);
    self.options = options;
    self._create();
  };
  constructor.prototype = prototype;
  $.fn[name] = function(method: string) {
    const args = Array.prototype.slice.call(arguments, 1);
    if (typeof method === 'string') this.each(function(this: HTMLElement) {
        const instance = $.data(this, name);
      if (instance && $.isFunction(instance[method])) instance[method].apply(instance, args);
    });
    else this.each(function(this: HTMLElement) {
      if (!$.data(this, name)) $.data(this, name, new constructor(method, this));
    });
    return this;
  };
};

export default widget;
