function gmailNormalize(email) {
  let [name, domain] = email.toLowerCase().split('@');
  [name, ] = name.replace('.', '').split('+');
  return name + '@' + domain;
}

db.user4.find({email: {$regex:/.*[+.].*@(protonmail\.com|protonmail\.ch|pm\.me|gmail\.com|googlemail\.com)$/}}).forEach(user => {
  const normalized = gmailNormalize(user.email);
  const verbatim = user.verbatimEmail || user.email;
  print(verbatim, '->', normalized);

  db.user4.update({
    _id: user._id
  }, {
    $set: {
      email: normalized,
      verbatimEmail: verbatim
    }
  });
});
