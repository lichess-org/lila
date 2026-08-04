// only delete team messages when the entire conversations is made of team messages

// TODO move mongosh log
// https://www.mongodb.com/docs/mongodb-shell/logs/location/#std-label-mongosh-log-location
// config.set("logLocation", "/dev/null") ? requires mongosh restart and is global

const until = new Date(Date.now() - 1000 * 3600 * 24 * 30); // 30 days ago
const teamTextRegex =
  /You received this (because you are subscribed to messages of the team|message because you are part of the team)/;

let total = 0;
let deleted = 0;

db.msg_thread.find({ 'lastMsg.date': { $lt: until } }, { _id: 1 }).forEach(thread => {
  total++;
  const selector = {
    tid: thread._id,
    text: {
      $not: teamTextRegex,
    },
  };
  const pms = db.msg_msg.countDocuments(selector, { limit: 1 });
  // console.log(`Thread ${thread._id} has ${pms} non-team messages`);
  if (pms === 0) {
    deleted++;
    db.msg_thread.deleteOne({ _id: thread._id });
    db.msg_msg.deleteMany({ tid: thread._id });
  }
  if (total % 1000 === 0) {
    print(`Processed ${total} threads, deleted ${deleted} threads`);
  }
});
