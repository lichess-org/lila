var initialFen = require('./util').initialFen;
var fixCrazySan = require('./util').fixCrazySan;

function renderNodesTxt(nodes) {
  if (!nodes[0]) return '';
  if (!nodes[0].san) nodes = nodes.slice(1);
  if (!nodes[0]) return '';
  var s = nodes[0].ply % 2 === 1 ? '' : Math.floor((nodes[0].ply + 1) / 2) + '... ';
  nodes.forEach(function(node, i) {
    if (node.ply === 0) return;
    if (node.ply % 2 === 1) s += ((node.ply + 1) / 2) + '. '
    else s += '';
    s += fixCrazySan(node.san) + ((i + 9) % 8 === 0 ? '\n' : ' ');
  });
  return s.trim();
}

function renderTree(node, variations, withTurn) {
  if (!node) return '';
  var isWhite = node.ply % 2 === 1;
  var txts = [''];
  if (isWhite || withTurn)
    txts[0] += Math.floor((node.ply + 1) / 2) + (isWhite ? '.' : '...') + ' ';
  txts[0] += fixCrazySan(node.san);
  if (node.children.length) txts[0] += ' ';
  variations.forEach(function(variation) {
    txts.push('(' + renderTree(variation, [], true) + ')');
  });
  txts.push(renderTree(node.children[0], node.children.slice(1)));
  return txts.join('');
}

module.exports = {
  renderFullTxt: function(ctrl) {
    var g = ctrl.data.game;
    var txt = renderTree(ctrl.tree.root.children[0], [], true).trim();
    var tags = [];
    if (g.variant.key !== 'standard')
      tags.push(['Variant', g.variant.name]);
    if (g.initialFen && g.initialFen !== initialFen)
      tags.push(['FEN', g.initialFen]);
    if (tags.length)
      txt = tags.map(function(t) {
        return '[' + t[0] + ' "' + t[1] + '"]';
      }).join('\n') + '\n\n' + txt;
    return txt;
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
      s += '<san>' + fixCrazySan(node.san) + '</san> ';
    });
    return s.trim();
  }
};
