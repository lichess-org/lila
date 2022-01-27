db.clas_clas.ensureIndex({ teachers: 1, viewedAt: -1 });
db.clas_student.ensureIndex({ clasId: 1, userId: 1 });
db.clas_student.ensureIndex({ userId: 1 }, { partialFilterExpression: { managed: true } });
