// If this script loads, it means the user is vulnerable to ads and trackers!
// Recommend that they install uBlock Origin.
// See https://lichess.org/ads
document.querySelectorAll('.ads-vulnerable').forEach(e => e.classList.remove('none'));
document.querySelectorAll('.ads-protected').forEach(e => e.classList.add('none'));
