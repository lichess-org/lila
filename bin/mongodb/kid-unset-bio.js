db.user4.updateMany({ kid: true, 'profile.bio': { $exists: true } }, { $unset: { 'profile.bio': 1 } });
