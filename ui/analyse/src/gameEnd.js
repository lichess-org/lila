module.exports = function(node) {
  if (node.dests !== '') return false;
  if (node.san.indexOf('#')) return 'mate';
  return 'draw';
};
