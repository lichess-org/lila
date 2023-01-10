const buffer = [];
const flush = () => {
  if (buffer.length) db.insight_int.insertMany(buffer, { ordered: false });
  buffer = [];
};
db.insight_int.drop();
db.insight.find().forEach(i => {
  i.m.forEach(m => {
    if (m.w) m.w = NumberInt(m.w * 1000);
    if (m.s) m.s = NumberInt(m.s * 1000);
  });
  buffer.push(i);
  if (buffer.length > 99) flush();
});
flush();
