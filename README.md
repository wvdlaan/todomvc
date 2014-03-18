# Quiescent / Light Table walkthrough

This walkthrough is based on a presentation that I gave at
the Amsterdam Clojure Meetup in March 2014.

The walkthrough uses [Light Table](http://www.lighttable.com/) and the
[Quiescent TodoMVC](https://github.com/levand/todomvc/tree/gh-pages/architecture-examples/quiescent)
to demonstrate what it feels like to develop a web-UI
with [Quiescent](https://github.com/levand/quiescent),
a lightweight ClojureScript abstraction over ReactJS.

To follow along you need to install
[leiningen](https://github.com/technomancy/leiningen),
git and Light Table.


## Open the TodoMVC application in your browser

1. Clone this github repo
2. Go to the repo with `cd todomvc`
3. Run `lein cljsbuild once`
4. Open `index.html` with a browser
5. Open the browser development window (shift-ctrl-i)
   and select the _console_ tab

After each transaction the application logs the transaction plus the
application state. So, just use the application and watch the log-messages
in the browser-console. This should give you an idea how your actions in
the UI relate to transactions and state-changes in the application.


## Open the TodoMVC application in a Light Table browser-tab

1. Leave your browser open and start Light Table.
2. Open a browser-tab with

     `ctrl-space` -> _Add connection_

     _Choose a client type_ -> _Browser_

     The browser-tab is displaying `about:blank` as can been seen
     at the bottom left.

3. Return to the browser to copy the URL
   (eg; on my desktop it's `file:///home/walter/todomvc/index.html`)
4. Paste the URL in Light Table to replace `about:blank` and press enter.

If you want you can open a browser console in Light Table with
`ctrl-space` -> _Console: Toggle console_.


## Open the Clojurescript code in a Light Table tab

You have now opened the todo-application in a Light Table browser-tab. Next we
will connect this application to the corresponding Clojurescript code.
First let's open the code in a new Light Table tab.

1. `ctrl-space` -> _Workspace: add folder_
2. Select the folder that you cloned from github
   (eg; on my desktop it's `/home/walter/todomvc`)
3. `ctrl-space` -> _Workspace: Toggle workspace tree_
   this will show/hide the workspace tree
4. In the workspace tree (sidebar on the left) open `application.cljs` which you
   will find in `todomvc/src/todomvc/application.cljs`

You now have two tabs in Light Table; `Quiescent TodoMVC` and `application.cljs`.
Let's put them side-by-side to get a better overview.

5. `ctrl-space` -> _Tabset: Add a tabset_
6. Drag one of the two tabs to the new tabset
7. Hide the workspace tree with `ctrl-space` -> _Workspace: Toggle workspace tree_

No further action is needed. The code in `application.cljs` is now connected to
the todo-application in the browser-tab.
Let's see what that brings us.


## Inspecting the application state

Go to the end of `application.cljs`. The last line reads `(def app-hook app)`. This
makes the application available outside the `main` function so we can use
this `app-hook` to sniff around in the application-state.

To evaluate expressions in Light Table you have to position the cursor right
after the expression and press `ctrl-enter`.
For example, type `@(:state app-hook)` on a newline at the end of
`application.cljs` and evaluate with `ctrl-enter`.
This will show the application-state.

If the result of the evaluation is too big for the screen Light Table will
only show the first bit. But if you click on the evaluation result Light
Table will expand it.

To look at the list of todo-items evaluate

```clojure
(:items @(:state app-hook))
```

If you get `[]` as an answer it means that your todo-list is empty.
Click on 'What needs to be done?' in the browser-tab and enter some
todo's. Now go back to the `application.cljs` tab, put
the cursor right after `(:items @(:state app-hook))` and press
`ctrl-enter` again.

This allows you the see how actions in the browser-tab cause changes in
the application state.


## Changing the application state

If you look a few lines up in `application.cljs` you will see
the expression `(swap! state transact transaction)` inside the
`init-updates` function.
This is the expression that processes the transactions coming in from
the UI.
We have already seen this as log-messages in the browser-console.

Since values in Clojure are immutable it is easy to run transactions
without changing the application state.
Let's try this by toggling the status of all items with:

```clojure
(transact/main @(:state app-hook) [:toggle-all])
```

Depending on the state of the application this will change `:all-done?`
to either `true` or `false`. Now, evaluate the expression again.
The value of `:all-done?` remains the same. This is because you are
not changing the actual state of the application. The actual state of
the application is stored in the `(:state app-hook)` atom.

Let's change the actual state by evaluating this expression:

```clojure
(swap! (:state app-hook) transact/main [:toggle-all])
```

That worked, the state has changed. And if you keep pressing
`ctrl-enter` you can see the value of `:all-done?` alternate between `true`
and `false`.


## Rendering the UI

But the change is not shown in the browser-tab.
To see the changed state reflected in the UI you must
render the application by evaluating this expression:

```clojure
(render/main app-hook)
```

Likewise you can add an item with

```clojure
(swap! (:state app-hook) transact/main [:add-item "More work"])
```

and show it in the browser-tab with `(render/main app-hook)`.

That's fun but a bit low level. The `init-updates` function creates a
go-block that will patiently wait for a transaction coming in through a
`core.async` channel.

You can put a transaction on this channel with

```clojure
(put! (:channel app-hook) [:add-item "More exercise"])
```

The browser-tab will automatically update as the transaction is
processed within the go-block.


## Render test

React, in combination with `.requestAnimationFrame`, will avoid much
of the rendering. Let's see this in action.

To remove the current todo's from the list run:

```clojure
(doseq [id (map :id (:items @(:state app-hook)))]
  (put! (:channel app-hook) [:remove-item id]))
```

And you can populate the table with generated items like this:

```clojure
(doseq [i (range 200)]
  (put! (:channel app-hook) [:add-item (str "Have fun " i)]))
```

Now we combine the adding and removing to see if our application does indeed
avoid most of the rendering.

```clojure
(let [num 200]
  ;; remove all todo's to get a clean start
  (doseq [id (map :id (:items @(:state app-hook)))]
    (put! (:channel app-hook) [:remove-item id]))
  ;; reset :next-id to 0
  (swap! (:state app-hook) assoc :next-id 0)
  ;; add items
  (doseq [id (range num)]
    (put! (:channel app-hook) [:add-item (str "Have fun " id)]))
  ;; remove items
  (doseq [id (range num)]
    (put! (:channel app-hook) [:remove-item id])))
```

On my machine I'm not seeing any intermediate rendering. So it seems like
React is doing its job.


## Rendering with Quiescent

All rendering is handled in `render.cljs`.
We are going to use Light Table to jump to the definition of `render/main`.

1. Move the cursor to any occurance of the string `render/main` within
   file `application.cljs`
2. `ctrl-space` -> _Editor: Jump to definition at cursor_

This will take you to the definition of `render/main`.
The keyboard shortcut for _Jump to definition_ is `ctrl-.` (control + dot).
Likewise, `ctrl-,` (control + comma) takes you back to the previous position.

`render/main` uses a boolean atom, `render-pending?`,
in combination with `.requestAnimationFrame` to make sure that the total
amount of renders will be less or equal to the browser refresh rate.

The function that is scheduled to perform the actual rendering is `q/render`.
This is the top-level Quiescent function.
It takes two arguments:

1. `(App @state channel)` will render the application UI
2. `(.getElementByIdj js/document "todoapp")` points to the DOM-element
   that will be controlled by React.
   You can find the definition of `todoapp` in `index.html` as element
   `<section id="todoapp"></section>`.


## Quiescent dom-elements

If you look at the definition of `App` in `render.cljs` you find several
calls to functions like `d/div`, `d/section`, `d/input`, etc.
These are Quiescent-funcions that represent dom-elements.
Open the elements-tab in your browser's development window and check for yourself
that there is a one-on-one relationship between the elements defined in `App` and
the dom-elements within `<section id="todoapp"></section>`.

Let's look, for example, at this expression at the end of the `Footer` component:

```clojure
(when (< 0 completed)
  (d/button {:id "clear-completed"
             :onClick #(go (>! channel [:clear-completed]))}
            (str "Clear completed (" completed ")")))
```

This defines a button that will only be included in the UI if the number of
`completed` items is larger than zero.
If the button is clicked the `[:clear-completed]` transaction is
pushed on the `core.async` channel.

Again, you can check this in the elements-tab of your browser.
Add an item, mark it as completed and do _inspect element_ on the
`Clear completed (1)` button that appears in the bottom right of the UI.
This element shows up exactly were it is defined in `render.cljs`,
at the end of `footer`.

You will not see the `:onClick` in the browser.
This is because events are handled in the React virtual dom.
For Chrome you can install _React Developer Tools_.
This will give you an extra Development Tool tab with React specific
information like, eg, the event handlers.

[Here](https://github.com/levand/quiescent/blob/master/docs.md#creating-virtual-dom-elements)
you can find more documentation on Quiescent dom-elements.


## Quiescent components

Functions like `App` and `TodoList` are Quiescent components defined
with `q/defcomponent`.
These act like any other Clojure function apart from two special requirements:

1. They all return a Quiescent dom-element as the function result
2. The first argument specifies the `state` relevant for the component

Some examples will help to clearify the second requirement.

The `Header` component is called with `nil` as first argument.
If you look at the definition of `Header` you will see why.
`Header` is not using anything from the application `state`.
As a result it will only be rendered once since no further rendering is needed.

An other example is `Footer`.
The first argument of `Footer` is `[current-filter items]`.
Both `current-filter` and `items` come from `state`.
They are passed to `Footer` in a vector because the first
argument of `Footer` must contain all `state`.

You could pass the complete `state` to `Footer`.
But `Footer` does not depend on `:all-done?`, so, sending the complete
`state` will cause unneeded rendering for `Footer`.

[Here](https://github.com/levand/quiescent/blob/master/docs.md#defining-components)
you can find more documentation on Quiescent-components.


## Changing the UI

As an exercise we will change `Header` such that the
text in the `new-todo` input changes to `"Anything more?"` if there
are unfinished todo's.

First we change `App` because it needs to send `all-done?` as an
argument to `Header`.

In `App` change this
```clojure
(Header nil channel)
```
to this
```clojure
(Header all-done? channel)
```

Now you have the evaluate `(d/defcomponent App ...)` with `ctrl-enter`.

Next we move to `Header` (press `ctrl-.` while the cursor is positioned
on `Header`) to let it receive `all-done?` as first argument.

In `Header` change this
```clojure
(q/defcomponent Header
  "The page's header, which includes the primary input"
  [_ channel]
```
to this
```clojure
(q/defcomponent Header
  "The page's header, which includes the primary input"
  [all-done? channel]
```

We also have to change the logic for `:placeholder`
so we change this
```clojure
:placeholder "What needs to be done?"
```
to this
```clojure
:placeholder (if all-done?
               "What needs to be done?"
               "Anything more?")
```

Now you have to evaluate `(d/defcomponent Header ...)` with `ctrl-enter`.

Enter some todo's in the list to check the new placeholder functionality.


## Compiling todomvc.js

Let's check this change in the browser.

1. Refresh the browser
2. Enter some items

Nothing has changed.
The new functionality for `:placeholder` has not reached the browser.

This is because `index.html` is using `todomvc.js` and Light Table
does not update this file.
Let's fix this by re-compiling `todomvc.js` with leiningen.

1. Save the changes you made to `render.cljs` in Light Table
   with `ctrl-s`
2. Recompile todomvc.js with `lein cljsbuild once`
3. Refresh the browser
4. Enter some items

You can save yourself some time during development by
running `lein cljsbuild auto`.
This instructs leiningen to automatically re-compile `todomvc.js` whenever
a source-file is saved.


## Application logic

The application logic is contained in `transact.cljs`.
As you will see _contained_ is the right word.
`transact.cljs` is completely ignorant about the UI.
It doesn't even know were the application state is stored.

As a result the code in `transact.cljs` can be moved from the browser
to the JVM simply by changing the filename from `transact.cljs`
to `transact.clj`.

This might not be impressive for the todomvc application but for a larger
application with a back-end it is a major advantage for it will allow you
to run integrated tests involving the back-end logic _and_ the front-end logic.

Open `transact.cljs` in a Light Table tab and go to the last line and evaluate
this expression to test the application logic:

```clojure
(try-transactions
 [[:add-item "bread"]
  [:add-item "butter"]
  [:toggle-item 10]])
```

Our next step is to run the same code on the JVM.
This will only work if you have a JVM installed on your machine.

If you don't have a JVM installed I would advice you to skip this last
step of the walkthrough because you won't see anything different.

I tried to save the file as `transact.clj` from Light Table using `ctrl-shift-s`.
But that does not work.
Light Table is smart enough to leave the file extension as it is.

1. Go to a terminal or file manager and copy `transact.cljs` to `transact.clj`
3. Right-click in the Light Table workspace tree and select _refresh folder_
2. Open `transact.clj`
3. Evaluate `transact.clj` with `ctrl-shift-enter`

Now we can run our test on the JVM.
Go to the last line of `transact.clj` and evaluate the same expression:

```clojure
(try-transactions
 [[:add-item "bread"]
  [:add-item "butter"]
  [:toggle-item 10]])
```
