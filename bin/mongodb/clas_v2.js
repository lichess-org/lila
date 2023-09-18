db.clas_clas.find({ createdAt: { $exists: 1 } }).forEach(clas => {
  db.clas_clas.update(
    { _id: clas._id },
    {
      $unset: { updatedAt: 1, createdAt: 1 },
      $set: { created: { by: clas.teachers[0], at: clas.createdAt } },
    },
  );
});
db.clas_student.find({ createdAt: { $exists: 1 } }).forEach(stu => {
  let teacher = db.clas_clas.findOne({ _id: stu.clasId }).teachers[0];
  db.clas_student.update(
    { _id: stu._id },
    {
      $unset: { updatedAt: 1, createdAt: 1 },
      $set: { created: { by: teacher, at: stu.createdAt } },
    },
  );
});
db.clas_student.find({ realName: { $exists: 0 } }).forEach(stu => {
  db.clas_student.update(
    { _id: stu._id },
    {
      $set: { realName: stu.userId, notes: '' },
    },
  );
});
