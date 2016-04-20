module.exports = {
  init: function(data, send, userId) {

    function myMember() {
      return userId ? data.members[userId] : null;
    }
    function owner() {
      return data.members.find(function(m) {
        return m.owner;
      });
    }

    var vm = {
      chapterId: myMember() ? myMember().chapterId : owner().chapterId
    };

    function addChapterId(data) {
      data.chapterId = vm.chapterId;
      return data;
    }

    return {
      chapterId: function() {
        return vm.chapterId;
      },
      path: function() {
        return myMember() ? myMember().path : owner().path;
      },
      setPath: function(path) {
        userId && send("setPath", addChapterId({path: path}));
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
