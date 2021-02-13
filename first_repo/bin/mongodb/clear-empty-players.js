var unset;
var count = 0;
db.game5.find({ $or: [{ p0: {} }, { p1: {} }] }).forEach(g => {
  unset = {};
  if (g.p0 && !Object.keys(g.p0).length) unset.p0 = true;
  if (g.p1 && !Object.keys(g.p1).length) unset.p1 = true;
  if (Object.keys(unset).length) {
    db.game5.update({ _id: g._id }, { $unset: unset });
    count++;
    if (count % 10000 === 0) print(count);
  }
});
print('Done ' + count);
