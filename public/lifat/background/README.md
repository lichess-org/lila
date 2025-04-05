### To create/update a background image gallery:

- First arrange the image files in `lifat/background/gallery`. The alphanumeric sort order of the filenames will become the gallery order.
- Then run `make-gallery.js`
- That script uses imagemagick to build `columns-2.webp` and `columns-4.webp`. These generated images are a montage of thumbnails rendered at 160x90 each in 2 & 4 column grids.
- It also spits out a `gallery.json` which is an ordered array of image asset URLs corresponding to gallery position.

### At runtime:

- When lila starts, it parses `gallery.json` once and includes that array in all dasher responses.
- The client delegates to old behavior if there's no gallery in the dasher params.
- If there is a gallery image array, the client constructs a URL to the right thumbnail background. Both 2 column and 4 column versions are around 1 kilobyte per thumbnail rendered at 160x90px.
- The client then creates a 2 or 4 column grid of divs that lighten their slice of the parent's gallery grid background image on hover. The divs also handle clicks and outline current selection.
