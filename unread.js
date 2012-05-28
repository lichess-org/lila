var userId = ObjectId("4cea2e4030c353af2b000000"); // thibault

var op = db.runCommand({
  mapreduce: "m_thread",
    query: {visibleByUserIds: userId},
    out: {inline:1},
    map: function() {
      var thread = this, nb = 0;
      thread.posts.forEach(function(p) {
        if (!p.isRead) {
          if (thread.creatorId.equals(ObjectId("4cea2e4030c353af2b000000"))) {
            if (!p.isByCreator) nb++;
          } else {
            if (p.isByCreator) nb++;
          }
        }
      });
      if (nb > 0) emit("n", nb);
    },
    reduce: function(key, values) {
      var sum = 0;
      for(var i in values) { sum += values[i]; }
      return sum;
    }
});

printjson(op);
