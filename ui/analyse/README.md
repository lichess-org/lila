Tree structure:

```js
[{
  "ply": 1,
  "san": "e4",
  "comments": [],
  "variations": []
}, {
  "ply": 2,
  "san": "d6",
  "comments": [],
  "variations": []
}, {
  "ply": 3,
  "san": "Nc3",
  "comments": [],
  "variations": [
    [{
      "ply": 3,
      "san": "Nf3",
      "comments": [],
      "variations": []
    }, {
      "ply": 4,
      "san": "Bg4",
      "comments": [],
      "variations": []
    }, {
      "ply": 5,
      "san": "Be2",
      "comments": [],
      "variations": [
        [{
          "ply": 5,
          "san": "Nc3",
          "comments": [],
          "variations": []
        }]
      ]
    }]
  ]
}, {
  "ply": 4,
  "san": "Nc6",
  "comments": [],
  "variations": []
},
...
]
```

Path structure:

```js
[{
  "ply": 3,
  "variation": 1
}, {
  "ply": 4,
  "variation": null
}]
```

Here, the user is on the fourth ply, on the first variation of the third ply.
