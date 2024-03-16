// prepare forum collections to allow "send diagnostics to lichess" feature

const makeId = () => {
  let id = '';
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  while (id.length < 8) {
    id += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return id;
};

const topic = {
  _id: makeId(),
  name: 'lichess',
  slug: 'lichess',
  userId: 'lichess',
  categId: 'diagnostic',
  createdAt: new Date(),
  updatedAt: new Date(),
  updatedAtTroll: new Date(),
  nbPosts: 1,
  nbPostsTroll: 1,
  troll: false,
  closed: false,
};

db.f_topic.insertOne(topic).insertedId;

const post = {
  _id: makeId(),
  text: '',
  troll: false,
  createdAt: new Date(),
  userId: 'lichess',
  author: 'lichess',
  topicId: topic._id,
  categId: 'diagnostic',
  number: 1,
  topic: 'none',
};

db.f_topic.updateOne({ _id: topic._id }, { $set: { lastPostId: post._id, lastPostIdTroll: post._id } });

const diagnostic = {
  _id: 'diagnostic',
  name: 'Diagnostic',
  desc: 'Your diagnostic reports',
  nbTopics: 1,
  nbPosts: 1,
  nbTopicsTroll: 1,
  nbPostsTroll: 1,
  lastPostId: post._id,
  lastPostIdTroll: post._id,
  hidden: true,
  quiet: true,
};

db.f_categ.insertOne(diagnostic);
