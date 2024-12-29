db.report2
  .find({ reason: { $exists: 1 } })
  .sort({ $natural: -1 })
  .forEach(r => {
    const sets = {};
    r.atoms.forEach((_, i) => {
      sets[`atoms.${i}.reason`] = r.reason;
    });
    db.report2.updateOne(
      { _id: r._id },
      {
        $unset: { reason: 1 },
        $set: sets,
      },
    );
  });
