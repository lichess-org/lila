reactions = Object.entries({
  PlusOne: '+1',
  MinusOne: '-1',
  Heart: 'heart',
  Laugh: 'laugh',
  Horsey: 'horsey',
  Thinking: 'thinking',
});

reactions.forEach(([bad, good]) => {
  badKey = 'reactions.' + bad;
  goodKey = 'reactions.' + good;
  posts = db.f_post.find({ [badKey]: { $exists: true } }).toArray();
  print(bad + ' ' + posts.length);
  print(posts.map(p => p._id));
  posts.forEach(p => {
    users = Array.from(new Set([...p.reactions[bad], ...(p.reactions[good] || [])]));
    db.f_post.update({ _id: p._id }, { $set: { [goodKey]: users }, $unset: { [badKey]: 1 } });
  });
});
