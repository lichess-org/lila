db.msg_thread.find().forEach(t => {
  db.msg_thread.update({ _id: t._id }, { $set: { 'maskWith.date': t.lastMsg.date } });
});
