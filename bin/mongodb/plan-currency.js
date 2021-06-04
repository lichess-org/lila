db.plan_charge.find({ cents: { $exists: 1 } }).forEach(c =>
  db.plan_charge.update(
    { _id: c._id },
    {
      $set: {
        money: {
          amount: NumberDecimal(c.cents * 100),
          currency: 'USD',
        },
        usd: NumberDecimal(c.cents * 100),
      },
      $unset: { cents: 1 },
    }
  )
);
