# Koukku

Koukku is a minimal npm React wrapper for hook based use.
It has no library dependencies apart from React.

Contains a macro based hiccup style rendering.

*WARNING* This is alpha/toy quality currently, for a mature and battle tested React wrapper please use Reagent.

## Example

Define components as pure functions that use React hooks.
Use `koukku.html/html` macro to output hiccup style HTML.

Like:

```clojure
(defn list-item [{name :name}]
  (h/html
    [:li name]))

(def items [{:name "item1"}
            {:name "item 2"}])

(defn listing [items]
  (let [[items set-items!] (react/useState items)]
    (h/html
      [:div.someclass
        [:ul
          (for [item items]
            (list-item item))]
        [:button {:onClick #(set-items! (conj items "new item"))}
         "add item"]])))
```
