coll = db.study_chapter_flat

function newUciCharPair(str){
  var last = str.substr(str.length - 1);
  return str.slice(0, -1) + last.replace("v", "õ").replace("u", "ô")
    .replace("t", "ó").replace("w", "ö")
    .replace("y", "ø").replace("x", "÷").replace("z", "ù");
}

function setFields(id, key, value) {

  var update = {$set:{}};    
  update.$set[key] = value;

  coll.update( {_id : id}, update );
}

function unsetFields(id, key){

  var update = {$unset:{}};    
  update.$unset[key] = "";

  coll.update( {_id : id}, update );
}


function fixDrops(id, root) {
    var newRoot = {}

    // Adding all nodes and drops to separate array
    var all = []
    var drops = []
    for (var i in root) {
      all.push(i)
      if(root[i].s && (root[i].s).includes("*"))
        drops.push(i)
    }
    var descDrops = drops.sort((a,b) => b.length - a.length);

    var changedVars = false
    for (var node in root){
      var newNode = node

      // We iterate over all paths ending with drops sorted from longest to shortest
      for(var dropNode of descDrops){
        // We always check whether current node, doesn't start with node ending with a drop
        if(node.startsWith(dropNode)){
          newNode = newNode.replace(dropNode, newUciCharPair(dropNode))
        }
      }

      // We need to fix variations too, if node has variations, we iterate over them
      // and check whether some of them aren't to be fixed
      if(root[node].o){
        var vars = []
        for(var os of root[node].o){
          if(descDrops.includes(node + os)){
            changedVars = true
            vars.push(newUciCharPair(os))
          }
          else vars.push(os)
        }
        root[node].o = vars
      }
      newRoot[newNode] = root[node]
    }
    unsetFields(id, "root");
    setFields(id, "root", newRoot);
}

coll.find({gamebook: true, createdAt: { $lt: ISODate("2021-02-19")}}).forEach(chap => {
  print(`${chap._id}`);
  fixDrops(chap._id, chap.root);
});