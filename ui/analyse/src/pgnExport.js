module.exports = {
  renderNodesTxt: function(nodes) {
    if (!nodes[0]) return '';
    if (!nodes[0].san) nodes = nodes.slice(1);
    if (!nodes[0]) return '';
    var s = nodes[0].ply % 2 === 1 ? '' : Math.floor((nodes[0].ply + 1) / 2) + '... ';
    nodes.forEach(function(node) {
      if (node.ply === 0) return;
      if (node.ply % 2 === 1) s += ((node.ply + 1) / 2) + '. '
      else s += '';
      s += node.san + ' ';
    });
    return s.trim();
  },
  renderNodesHtml: function(nodes) {
    if (!nodes[0]) return '';
    if (!nodes[0].san) nodes = nodes.slice(1);
    if (!nodes[0]) return '';
    var s = nodes[0].ply % 2 === 1 ? '' : Math.floor((nodes[0].ply + 1) / 2) + '...&nbsp;';
    nodes.forEach(function(node) {
      if (node.ply === 0) return;
      if (node.ply % 2 === 1) s += ((node.ply + 1) / 2) + '.&nbsp;'
      else s += '';
      s += '<san>' + node.san + '</san> ';
    });
    return s.trim();
  }
};
