# Koukku

Koukku is a minimal npm React wrapper for hook based use.
It has no library dependencies apart from React.

Contains a macro based hiccup style rendering.

*WARNING* This is alpha/toy quality currently, for a mature and battle tested React wrapper please use Reagent.

# API

## koukku.core namespace

`koukku.core/main` entrypoint takes a function component and an element
and renders the component. The element may be an element id string or an
actual DOM element. If it is an id, the element will be created and added
to the body if it doesn't exist.

`koukku.core/use-state` wraps React `useState` hook.

`koukku.core/use-effect` wraps React `useEffect` hook.

`koukku.core/use-reducer` wraps React `useReducer` hook.
It wraps the given reducer in a function so that any `ifn?` with 2-arity
can be used as a reducer, for example a multimethod.

## koukku.html namespace

The html namespace is the JSX equivalent and works very similar
to hiccup in Reagent, but with some restrictions. The html is
based on a macro and tries to do as much work in compile time.

`koukku.html/html` takes hiccup style markup and yields code that
returns a React Element (by calling `react/createElement`).

The basic form of markup contains a dom element, attributes
and children, for example:
```clojure
[:div#thediv.someclass {:on-click #(js/alert "click")}
  [:ul
    [:li "first list item"]
    [:li "second list item"]]]
```

The element name may contain an id (after `#`) and class names (each starting with `.`).

Calling other ClojureScript functions as components is in the
form

```clojure
[component-fn ...args...]
```

Calling JS React components is done with:
```
[:> SomeJSComponent {:some-attr value} ...children...]
```

Creating fragments can be done with the pseudo element name `:<>`.

### Special elements for control flow

The html generation includes special elements for regular control flow
for convenience.

Included special elements are:
- `:koukku.html/if`
- `:koukku.html/when`
- `:koukku.html/for`

The special elements work like in clojure except the body is compiled as
html as well. This is often convenient as breaking into Clojure code you
need to call `koukku.html/html` to get back into html templating.

```clojure
(h/html
 [:ul
  [::h/for [item ["one" "two" "three"]]
   ^{:key item}
   [:li item]]])
```

is equivalent to the Clojure code:
```clojure
(h/html
 [:ul
   (into-array
     (for [item ["one" "two" "three"]]
       (h/html
         ^{:key item}
         [:li item])))])
```


## Examples

See todomvc example in examples/todomvc/ folder.

Define components as pure functions that use React hooks.
Use `koukku.html/html` macro to output hiccup style HTML.

Like:

```clojure
(defn list-item [{name :name}]
  (h/html
    [:li name]))

(def items [{:name "item1" :id 1}
            {:name "item 2" :id 2}])

(defn listing [items]
  (let [[items set-items!] (react/useState items)]
    (h/html
      [:div.someclass
        [:ul
          [::h/for [{id :id :as item} items]
            ^{:key id}
            [list-item item]]]
        [:button {:onClick #(set-items! (conj items {:name "new item"}))}
         "add item"]])))

(defn main-component []
  (h/html [listing items]))

(k/main main-component "app")
```
