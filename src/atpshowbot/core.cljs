(ns atpshowbot
  (:require [reagent.core :as reagent :refer [atom]]))


;(def state (atom {}))

(def state (atom {
                  :votes {"title" {:votes #{"nick1" "nick2"} :author "nick1"}}
                  :links [["http://foo.com" "nick1"] ["another link" "nick2"]]
                  }))

(defn vote-tally [state]
  (let [titles-by-votes (-> (for [[title {nicks :votes author :author}] (:votes state)] [(count nicks) title author]) sort reverse)]
    (for [[votes title author] titles-by-votes]
      [:tr [:td votes] [:td title] [:td author]])))

(defn root []
  [:div
   [:h1 "atpshowbot"]

   [:h1 "Titles"]

   [:table
    [:tr
     [:th "Votes"]
     [:th "Title"]
     [:th "Suggested by"]]
    (vote-tally @state)]

   [:h1 "Links"]

   [:table
    [:tr
     [:th "Link"]
     [:th "Suggested by"]]

    (for [[link nick] (:links @state)]
     [:tr
      [:td [:a {:href link} link]]
      [:td nick]])

   ]

    [:p
        [:h2 "IRC commands"]
        [:div "!s {title} - suggest a title."]
        [:div "!v {prefix of title} - vote on a title"]
        [:div "!state - show the number of votes of the suggested titles"]
        [:div "!l {URL} - suggest a link."]
        [:div "!ll - list links"]
        [:div "!h - see this message."]]

   ])

(defn ^:export run []
  (reagent/render-component [root]
                            (.-body js/document)))
