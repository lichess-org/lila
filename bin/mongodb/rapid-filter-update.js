var speedId = 5;
[db.config, db.config_anon].forEach(coll => {
  coll.update(
    { 'filter.s.2': { $exists: true } },
    { $push: { 'filter.s': NumberInt(speedId) } },
    { multi: true },
  );
});
