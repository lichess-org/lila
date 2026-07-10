db.currentOp({ op: 'query', active: true, secs_running: { $gt: 2 }, ns: 'lichess.analysis2' });
