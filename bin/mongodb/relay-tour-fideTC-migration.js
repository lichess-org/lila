db.relay_tour.updateMany({ 'info.fideTc': { $exists: true } }, { $rename: { 'info.fideTc': 'info.fideTC' } });
