var u = 'neio'.toLowerCase();
var since = new Date('2019-01-17');

function monthDiff(dateFrom, dateTo) {
  return dateTo.getMonth() - dateFrom.getMonth() + 12 * (dateTo.getFullYear() - dateFrom.getFullYear());
}

db.user4.update(
  { _id: u },
  { $set: { 'plan.months': NumberInt(monthDiff(since, new Date()) + 1), 'plan.since': since } },
);
