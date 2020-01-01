let fields = ['engine', 'booster', 'troll', 'ipBan', 'reportban', 'rankban']
let selector = [];
let projection = {};
for (field of fields) {
  selector[field] = true;
  let sel = {};
  sel[field] = true;
  selector.push(sel);
  projection[field] = true;
}
printjson(selector);
printjson(projection);

db.user4.find({
  $or: selector,
  marks: {$exists: false}
},projection).forEach(u => {
  let marks = [];
  for(field of fields) {
    if (u[field]) marks.push(field.toLowerCase());
  }
  db.user4.update({_id:u._id},{$set:{marks:marks}});
});
