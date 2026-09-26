const until = new Date(Date.now() - 1000 * 3600 * 24 * 30 * 2); // 2 months ago
const teamTextRegex =
  /You received this (because you are subscribed to messages of the team|message because you are part of the team)/;

let total = 0;

function* group(size) {
  let batch = [];
  while (true) {
    const element = yield;
    if (!element) {
      yield batch;
      return;
    }
    batch.push(element);
    if (batch.length >= size) {
      let element = yield batch;
      batch = [element];
    }
  }
}

const grouper = group(1000);

db.msg_msg
  .find({ text: teamTextRegex, date: { $lt: until } }, { date: 1 })
  .skip(0)
  .forEach(msg => {
    const batch = grouper.next(msg._id).value;
    if (batch) {
      total += batch.length;
      db.msg_msg.deleteMany({ _id: { $in: batch } });
      const delay = db.tmp_stuff.findOne({ _id: 'team-delete-delay' }).delay;
      print(`${msg.date} | ${total} (${delay}ms)`);
      sleep(delay); // preserve the oplog
    }
  });
