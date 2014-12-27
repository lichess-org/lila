module.exports = function(messages) {
  return function(key) {
    var str = messages[key] || untranslated[key] || key;
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };
};
