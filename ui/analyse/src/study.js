module.exports = {
  init: function(data) {

    var vm = {
      chapterId: data.ownerChapterId
    };

    return {
      currentChapterId: function() {
        return vm.chapterId;
      }
    };
  }
};
