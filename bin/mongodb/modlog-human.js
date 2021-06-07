db.modlog.update({ mod: { $ne: 'lichess' } }, { $set: { human: true } }, { multi: true });

db.modlog.createIndex({ mod: 1, date: -1 }, { partialFilterExpression: { human: true } });
