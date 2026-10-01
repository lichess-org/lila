function dropAndWriteLegacyStatus() {
  db.legacy_title.drop();

  function isPrivate(note) {
    return /private|anon/im.test(note);
  }
  function isPublic(note) {
    return /public/im.test(note);
  }

  db.user4
    .find({
      // _id: 'vagoff',
      enabled: true,
      title: {
        $exists: true,
        $nin: ['LM', 'BOT'],
      },
    })
    .forEach(user => {
      const hasReq = db.title_request.countDocuments({ _id: user._id, 'history.status.n': 'approved' });
      if (!hasReq) {
        const notes = db.note.distinct('text', { to: user._id, mod: true });
        const status = notes.some(isPrivate) ? 'private' : notes.some(isPublic) ? 'public' : 'unknown';
        db.legacy_title.insertOne({
          _id: user._id,
          user: {
            username: user.username,
            title: user.title,
            profile: user.profile,
          },
          notes,
          status,
        });
      }
    });

  print('Done. ' + db.legacy_title.countDocuments() + ' legacy title users found.');
}

// function readPublicFideIds() {
//   db.legacy_title.find({ status: 'public' }).forEach(user => {
//     let fideId = 0;
//     notes.forEach(note => {
//       if (fideId) return;
//       const match = note.match(/ratings\.fide\.com\/profile\/(\d+)/);
//       fideId = match[1];
//     });
//     console.log(user._id);
//     console.log(user.notes);
//   });
// }

function printStats() {
  print('Private: ' + db.legacy_title.countDocuments({ status: 'private' }));
  print('Public: ' + db.legacy_title.countDocuments({ status: 'public' }));
  print('Unknown: ' + db.legacy_title.countDocuments({ status: 'unknown' }));
}

dropAndWriteLegacyStatus();
printStats();
// readPublicFideIds();
