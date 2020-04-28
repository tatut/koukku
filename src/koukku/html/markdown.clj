(ns koukku.html.markdown
  "Implement compiler for markdown using commonmark library"
  (:import (com.vladsch.flexmark.parser Parser)
           (com.vladsch.flexmark.html HtmlRenderer)
           (org.jsoup Jsoup)
           (org.jsoup.nodes Element Comment DataNode DocumentType TextNode XmlDeclaration))
  (:require [clojure.string :as str]))

(defn parse [str]
  (->  (Parser/builder) .build (.parse str)))

(defn md->html [doc]
  (-> (HtmlRenderer/builder) .build (.render doc)))

(defn html->elements [html]
  (-> html Jsoup/parseBodyFragment .body .children))

;; 1. combine all children to text
;; 1.1 replacing forms with special identifiers we can find in text later
;; 1.2 so '("some *markdown* " [hiccup-comp ...] " and more stuff.")
;;     will become simple string "some *markdown* KFfr61c91x0 and more stuff."
;;     where KFfr61c91x0 is a generated random identifier
;; 2. parse and render markdown to html
;; 3. parse generated html with jsoup
;; 4. recursively create react element forms from jsoup dom
;; 4.1 replacing form identifiers in test with actual child components

(let [chars (str/join
             (map char
                  (concat (range (int \A) (inc (int \Z)))
                          (range (int \a) (inc (int \z)))
                          (range (int \0) (inc (int \9))))))
      random-id (fn [] (str "KF" (str/join (repeatedly 16 #(rand-nth chars)))))]
  (defn form-with-random-id [{:keys [forms text]} new-value]
    (let [new-id (first (drop-while #(contains? forms %)
                                    (repeatedly random-id)))]
      {:forms (assoc forms new-id new-value)
       :text (str text new-id)})))

(defn prepare-body [body]
  (reduce (fn [acc form]
            (if (string? form)
              (update acc :text str form)
              (form-with-random-id acc form)))
          {:text ""
           :forms {}}
          body))

(defmulti element->hiccup (fn [_forms element] (type element)))

(defmethod element->hiccup Element [forms element]
  (let [tag (.tagName element)
        id (.id element)
        classes (.classNames element)
        tag (keyword
             (str tag
                  (when-not (str/blank? id)
                    (str "#" id))
                  (str/join ""
                            (map #(str "." %) classes))))
        attrs (into {}
                     (keep (fn [attr]
                             (let [key (.getKey attr)
                                   val (.getValue attr)]
                               (when (and (not= key "id")
                                          (not= key "class"))
                                 [key val]))))
                     (-> element .attributes .asList))]
    [(into (if (seq attrs)
              [tag attrs]
              [tag])
            (mapcat (partial element->hiccup forms))
            (.childNodes element))]))

;; Pattern to return text before, the form reference and text after
(def form-reference-pattern #"^(.*?)((KF[\w\d]{16})(.*))?$")

(defmethod element->hiccup TextNode [forms element]
  (loop [acc []
         [txt before _ form-ref after :as m] (re-find form-reference-pattern (.text element))]
    (if-not form-ref
      (if (str/blank? before)
        acc
        (conj acc before))
      (let [form (get forms (str/trim form-ref))]
        (recur (into acc [before form])
               (re-find form-reference-pattern after))))))

(defmethod element->hiccup :default [_ element]
  ;; Only element and textnode implement, shouldn't need others
  nil)

(defn compile-md [body]
  (let [{:keys [forms text]} (prepare-body body)
        elements (-> text parse md->html html->elements)]
    (into [:<>]
          (mapcat (partial element->hiccup forms))
          elements)))

(def test-md "## My section\nThis is a *very nice* section. A list:\n- item 1\n- second item")
