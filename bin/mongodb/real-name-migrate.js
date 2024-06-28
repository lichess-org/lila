const sel = { $or: [{ 'profile.firstName': { $exists: true } }, { 'profile.lastName': { $exists: true } }] };

db.user4
  .find(sel, { profile: 1 })
  .limit(1000)
  .forEach(function (user) {
    const fullName = ((user.profile.firstName || '') + ' ' + (user.profile.lastName || ''))
      .trim()
      .replace(/\s+/g, ' ');
    db.user4.updateOne(
      { _id: user._id },
      {
        $set: { 'profile.realName': fullName },
        $unset: { 'profile.firstName': true, 'profile.lastName': true },
      },
    );
  });
