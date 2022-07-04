const cpToWinC = cp => 2 / (1 + Math.exp(-0.004 * cp)) - 1;
const cpToWinP = cp => 50 + 50 * cpToWinC(cp);
const winDiffToAccP = winDiff =>
  Math.min(100, Math.max(0, 103.1668100711649 * Math.exp(-0.04354415386753951 * winDiff) + -3.166924740191411));
const encode = d => Math.round(10 * d);

const mateCp = 1000;

db.insight_prod.find({ a: true, 'm.a': { $exists: false } }).forEach(insight => {
  insight.m.forEach((m, i) => {
    const evalCp = (m.e = Math.max(-mateCp, Math.min(mateCp, m.m ? m.m * mateCp : m.e)));
    const winP = cpToWinP(evalCp);
    m.w = encode(winP);
    if (typeof m.c == 'number') {
      const cpl = m.c;
      const nextWinP = cpToWinP(evalCp - cpl);
      m.a = encode(winDiffToAccP(winP - nextWinP));
      // console.log(`${i} ${evalCp} ${cpl} ${evalCp - cpl} ${winP}->${nextWinP} ${winDiffToAccP(winP - nextWinP)}`);
    } else if (i == insight.m.length - 1 && m.m == 1 && insight.p != 13) {
      // checkmate
      m.a = encode(100);
    }
    delete m.m;
  });
  // printjson(insight.m.map(m => `${typeof m.e == 'undefined' ? '#' + m.m : m.e} -> ${m.w} | ${m.c} -> ${m.a}`));
  // db.insight.updateOne({ _id: insight._id }, { $set: { m: insight.m } });
  db.insight_js.insertOne(insight);
});
