# Quiescent / Light Table walkthrough

This walkthrough is based on a presentation that I recently gave at
the Amsterdam Clojure Meetup.

The walkthrough uses [Light Table](http://www.lighttable.com/) and the
[Quiescent TodoMVC](https://github.com/levand/todomvc/tree/gh-pages/architecture-examples/quiescent)
to demonstrate what it feels like to develop a web-UI
with [Quiescent](https://github.com/levand/quiescent).

A personal goal is to gain experience with both Light Table and Quiescent.
So, any help on how the workflow that I describe can be improved is highly appreciated.

To follow along you need to install [leiningen](https://github.com/technomancy/leiningen),
git and Light Table.

## Get the TodoMVC application running on your machine

1. Clone this github repo
2. Go to the repo with `cd todomvc`
3. Run `lein cljsbuild once`
4. Open `index.html` with a browser
5. Open the browser console (`ctrl-shift-i` in Chrome)

After each transaction the application logs the transact plus the
application state. So, just use the application and watch the log-messages
in the browser-console. This should give you an idea how your actions in
the UI relate to transactions and state-changes in the application.

## Open the TodoMVC application in a Light Table browser-tab

1. Leave your browser open and start Light Table.
2. Open a browser-tab with

     `ctrl-space` -> _Add connection_

     _Choose a client type_ -> _Browser_

     You are connection to `about:blank` as can been seen at the bottom of
     the browser-tab.

3. Return to the browser to copy the URL
   (eg; on my desktop it's `file:///home/walter/todomvc/index.html`)
4. Paste the URL in Light Table to replace `about:blank` and press enter.

# Open the Clojurescript code in a Light Table tab

You have now opened the todo-application in a Light Table browser-tab. Next we
will connect this application to the corresponding Clojurescript code.
First let's open the code in a new Light Table tab.

1. `ctrl-space` -> _Workspace: add folder_
2. Select the folder that you cloned from github
   (eg; on my desktop it's `/home/walter/todomvc`)
3. `ctrl-space` -> _Workspace: Toggle workspace tree_
   this will show/hide the workspace tree
4. In the workspace tree (sidebar on the left) open `core.cljs` which you
   will find in `todomvc/src/todomvc/core.cljs`

You now have two tabs in Light Table; `Quiescent TodoMVC` and `core.cljs`
Let's put them side-by-side to get a better overview.

5. `ctrl-space` -> _Tabset: Add a tabset_
6. Drag one of the two tabs to the new tabset
7. Hide the workspace tree with `ctrl-space` -> _Workspace: Toggle workspace tree_

No further action is needed. The code in `core.cljs` is now connected to
the todo-application in the browser-tab.
Let's see what that brings us.

## Inspecting the application state

Go to the end of `core.cljs`. The last line reads `(def app-hook app)`. This
makes the application available outside the `main` function so we can use
this `app-hook` to sniff around in the application-state.

To evaluate expressions in Light Table you have to position the cursor right
after the expression and press `ctrl-enter`.
For example, type `@(:state app-hook)` on a newline at the end of
`core.cljs` and evaluate with `ctrl-enter`.
This will show the application-state.

To look at the list of todo-items evaluate

```clojure
(:items @(:state app-hook))
```

If you get `[]` as an answer it means that your todo-list is empty.
Click on 'What needs to be done?' in the browser-tab and enter some
todo's. Now go back to `core.cljs` tab, put
the cursor right after `(:items @(:state app-hook))` and press
`ctrl-enter` again.

This allows you the see how actions in the browser cause changes in
the application state.

## Changing the application state

If you look a few lines up in `core.cljs` you'll see
the expression `(swap! state transact transaction)` inside the
`init-updates` function.
This is the expression that processes the transactions coming in from
the UI.
We have already seen this as log-messages in the browser-console.

Since values in Clojure are immutable it is easy to run transactions
without changing the application state.
Let's try this by toggling the status of all items with:

```clojure
(data/transact @(:state app-hook) [:toggle-all])
```

Depending on the state of your application this will change `:all-done?`
to either `true` or `false`. Now, evaluate the expression again.
The value of `:all-done?` remains the same. This is because you are
not changing the actual state of the application. The actual state of
the application is stored in the `(:state app-hook)` atom.

Let's change the actual state by evaluating this expression:

```clojure
(swap! (:state app-hook) data/transact [:toggle-all])
```

That worked, the state has changed. And if you keep pressing
`ctrl-enter` you can see the value of `:all-done?` alternate between `true`
and `false`.

## Rendering the UI

But the change is not shown in the browser-tab. Let's
render the application by evaluating this expression:

```clojure
(render/request-render app-hook)
```

Likewise you can add an item with

```clojure
(swap! (:state app-hook) data/transact [:add-item "More work"])
```

and show it in the browser-tab with `(render/request-render app-hook)`.

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

React, combination with `.requestAnimationFrame`, will avoid much
of the rendering. Let's test that this is true.

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

That took a noticable amount of time on my machine. Let's now combine the
adding and removing to see if our application does indeed avoid most of the
rendering.

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
React is doing its job! That's it for now. I hope you enjoyed the walkthrough.
