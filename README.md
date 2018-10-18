# ajax-homework

Logic: `src/ajax_homework/core.cljs`
Page: `resources/public/index.html`
Style: `resources/public/css/style.css`

As usual the app looks hideous. Even though I specifically planned to leave Monday for the styling, I ran into so many excruciating tooling issues that it never happened. So it's styled just enough to be legible.

For this one I tried out the ClojureScript library `reagent`, which is a thin wrapper for React. It provides a variant of Clojure's atoms (immutable containers of mutable state, similar to the store in Redux) that render and reconcile the page upon updating. I didn't bother with the action/reducer architecture this time. The other new thing I tried is `specter`, a library for manipulating nested data structures (something which is surprisingly painful with the Clojure standard library).

I've been running into problems deploying to GitHub Pages, and based on Stephen's suggestion I'm going to try nuking the whole thing, git data included. (It's the only way to be sure....) So it will look like the homework was late.