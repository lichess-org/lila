const insight = connect('mongodb://localhost:27318/insight');
const sec = connect('mongodb://localhost:27117/lichess');

console.log('Counting marked users...');

const markedSelector = { marks: { $in: ['engine', 'boost'] } };
const nbMarked = sec.user4.countDocuments(markedSelector);

console.log(`Found ${nbMarked} marked users. Starting cleanup...`);

let inspected = 0;
let deleted = 0;

sec.user4.find(markedSelector, { _id: 1 }).forEach(u => {
  deleted += insight.insight.deleteMany({ u: u._id }).deletedCount;
  inspected += 1;
  if (inspected % 1000 === 0) {
    console.log(`Processing user ${inspected}/${nbMarked} / Deleted ${deleted} insight entries`);
  }
});
