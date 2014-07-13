(ns atpshowbot
  (:require [reagent.core :as reagent :refer [atom]]
            [ajax.core :refer [GET POST]]))


(def state (atom {}))

;; Example state for testing
;(def state (atom {
;                  :votes {"title" {:votes 3, :author "nick1", :did-vote true}}
;                  :links [["http://foo.com" "nick1"] ["another link" "nick2"]]
;                  }))

(defn got-state [response]
  (reset! state response))

(defn vote! [title]
  (GET "/vote" {:params {:title title} :handler got-state}))

(defn vote-tally [state]
  (let [titles-by-votes (-> (for [[title {votes :votes, did-vote :did-vote, author :author}] (:votes state)] [votes did-vote title author]) sort reverse)]
    (for [[votes did-vote title author] titles-by-votes]
      ^{:key title} [:tr
       [:td votes]
       (if did-vote
         [:td]
         [:td.vote-button {:on-click #(vote! title)} "+ 1"])
       [:td title]
       [:td author]])))

(defn update-state []
  ;; Update the state every 10 seconds
  (js/setTimeout #(GET "/state" {:handler got-state}) 10000))

(defn root []
  (update-state)
  [:div.top
   [:h1 "Accidental Tech Podcast Showbot"]

   [:h2 "Titles"]

   [:table
    [:tr
     [:th "Votes"]
     [:th]
     [:th "Title"]
     [:th "Suggested by"]]
    (vote-tally @state)]

   [:h2 "Links"]

   [:table
    [:tr
     [:th "Link"]
     [:th "Suggested by"]]

    (for [[link nick ip] (:links @state)]
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
  (GET "/state" {:handler got-state})
  (reagent/render-component [root]
                            (.-body js/document)))
