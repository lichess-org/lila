db.team.find().forEach(t => {

  let c = db.chat.findOne({_id:t._id});

  if (c) {
    db.chat.remove({_id: c._id});
    c._id = `t:${c._id}`;
    db.chat.insert(c);
    print(`Moved ${c._id}`);
  }
});
