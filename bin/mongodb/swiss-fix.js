db.swiss.find({featurable:true,startsAt:{$lt:new Date(Date.now() - 1000 * 3600 * 24)},'settings.i':{$lt:3600 * 24}}).forEach(s => {
  print(`${s._id} ${s.name} round:${s.round}`);
  db.swiss.update({_id:s._id},{
    $set:{
      finishedAt: new Date(),
      'settings.n': s.round,
      canceled: new Date()
    },
    $unset:{
      nextRoundAt: 1,
      featurable: 1
    }
  });
});
