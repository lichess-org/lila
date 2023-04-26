db.relay.updateMany({ 'sync.delay': { $exists: 1 } }, { $rename: { 'sync.delay': 'sync.period' } });
