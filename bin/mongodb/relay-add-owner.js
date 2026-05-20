const owner = 'Javlonbek2418'.toLowerCase();
const tourIds = 'wxjnFxN5 G8ZFMIJJ tO0H69qH'.split(' ');

db.relay_tour.updateMany({ _id: { $in: tourIds } }, { $addToSet: { ownerIds: owner } });
