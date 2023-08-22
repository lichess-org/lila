var now = new Date();
var inOneMonth = new Date(ISODate().getTime() + 1000 * 60 * 60 * 24 * 31);

db.plan_patron.remove({});
db.plan_charge.remove({});
db.user4.update(
  {
    plan: {
      $exists: true,
    },
  },
  {
    $unset: {
      plan: true,
    },
  },
  {
    multi: true,
  },
);

var userIds = [];

function donationToPatron(donation) {
  var charge = {
    _id: donation._id,
    date: donation.date,
    cents: donation.gross,
    payPal: {},
  };
  if (donation.userId) charge.userId = donation.userId;
  if (donation.payPalTnx) charge.payPal.tnxId = donation.payPalTnx;
  if (donation.payPalSub) charge.payPal.subId = donation.payPalSub;
  if (donation.name) charge.payPal.name = donation.name;
  if (donation.email) charge.payPal.email = donation.email;
  db.plan_charge.insert(charge);

  if (userIds.indexOf(donation.userId) !== -1) return;
  userIds.push(donation.userId);

  var patron = {
    _id: donation.userId,
    payPal: {
      lastCharge: donation.date,
    },
    lastLevelUp: now,
    expiresAt: inOneMonth,
  };
  if (donation.payPalSub) patron.payPal.subId = donation.payPalSub;
  if (donation.email) patron.payPal.email = donation.email;
  db.plan_patron.insert(patron);
  db.user4.update(
    {
      _id: donation.userId,
    },
    {
      $set: {
        plan: {
          active: true,
          months: NumberInt(1),
          since: donation.date,
        },
      },
    },
  );
}

db.donation
  .find({
    userId: {
      $exists: true,
    },
  })
  .sort({
    date: -1,
  })
  .forEach(donationToPatron);

donationToPatron({
  userId: 'thibault',
  date: new Date(ISODate().getTime() - 1000 * 60 * 60 * 24 * 31 * 3),
  email: 'lichess.contact@gmail.com',
});
