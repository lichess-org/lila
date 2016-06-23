module.exports = {
  incrementalId: function(obj, it) {
    obj.id = it + 1;
    return obj;
  }
};
