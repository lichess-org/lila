module.exports = {
  init: function(data, send) {

    var vm = {
      chapterId: data.ownerChapterId
    };

    function addChapterId(data) {
      data.chapterId = vm.chapterId;
      return data;
    }

    return {
      currentChapterId: function() {
        return vm.chapterId;
      },
      deleteVariation: function(path) {
        send("deleteVariation", addChapterId({path: path}));
      },
      promoteVariation: function(path) {
        send("promoteVariation", addChapterId({path: path}));
      }
    };
  }
};
