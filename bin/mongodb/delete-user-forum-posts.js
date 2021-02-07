var user = 'colin_ni';
db.f_post.update({ userId: user }, { $set: { text: '', erasedAt: new Date() } }, { multi: 1 });
