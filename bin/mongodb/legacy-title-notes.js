prodDb = connect(`mongodb://localhost:27117/lichess`);

function dropAndWriteLegacyStatus() {
  db.legacy_title.drop();

  function isPrivate(note) {
    return /private|anon/im.test(note);
  }
  function isPublic(note) {
    return /public/im.test(note);
  }

  let nb = 0;
  prodDb.user4
    .find({
      enabled: true,
      title: {
        $exists: true,
        $nin: ['LM', 'BOT'],
      },
    })
    .forEach(user => {
      nb++;
      const reqs = prodDb.title_request
        .find({
          userId: user._id,
          'history.status.n': 'approved',
        })
        .toArray();
      print('Processing ' + nb + ' / ' + user.username + ' / reqs: ' + reqs.length);
      const notes = prodDb.note.distinct('text', { to: user._id, mod: true });
      const status = notes.some(isPrivate) ? 'private' : notes.some(isPublic) ? 'public' : 'unknown';
      let fideId = undefined;
      notes.forEach(note => {
        if (fideId) return;
        const match = note.match(/fide\.com\/(profile\/|card\.phtml\?event=)(\d+)/);
        fideId = match && match[2];
      });
      db.legacy_title.insertOne({
        _id: user._id,
        user: {
          username: user.username,
          title: user.title,
          profile: user.profile,
          seenAt: user.seenAt,
          count: user.count,
          tournamentsPoints: user.toints,
        },
        notes,
        guessedStatus: status,
        fideIdFromNotes: fideId,
        approvedRequests: reqs.map(r => r.data),
      });
    });

  print('Done. ' + db.legacy_title.countDocuments() + ' legacy title users found.');
}

function printStats() {
  print('Private: ' + db.legacy_title.countDocuments({ status: 'private' }));
  print('Public: ' + db.legacy_title.countDocuments({ status: 'public' }));
  print('Unknown: ' + db.legacy_title.countDocuments({ status: 'unknown' }));
}

dropAndWriteLegacyStatus();
printStats();
