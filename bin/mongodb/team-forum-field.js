db.team.updateMany({}, { $set: { forum: NumberInt(20) }, $unset: { hideForum: true } });
