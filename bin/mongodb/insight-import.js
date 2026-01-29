const insight = connect('mongodb://localhost:27318/insight');

insight.insight
  .find()
  .limit(999999)
  .forEach((doc) => {
    try {
      db.insight.insertOne(doc);
    } catch (e) {}
  });
