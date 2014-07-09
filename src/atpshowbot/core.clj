(ns atpshowbot.core
  (:require
   [irclj.core :refer :all]
   [irclj.parser :refer :all]
   [clojure.algo.generic.functor :refer :all]
   [ring.adapter.jetty :as jetty]
   [compojure.route :as route]
   [compojure.core :refer [defroutes GET POST]]
   [ring.util.response :as resp]
   ))

; - Constants -
(def channel "#atp")
(def server "irc.freenode.org")
(def port 6667)
(def bot-nick "atpbot-boxed")
;(def bot-nick-password "")


; - State, with example structure -
(def state (atom {:votes {}, :links []}))
;; {
;;  :votes {"title" {:votes #{"nick1" "nick2"} :author "nick1"}}
;;  :links [["link" "nick1"] ["another link" "nick2"]]
;; }


; - Pure functions -
(defn titles-with-prefix [state prefix]
  (filter #(.startsWith % prefix) (keys (:votes state))))

(defn normalize-title [title]
  (-> title
      .toLowerCase
      .trim
      (clojure.string/replace #"\.+$" "")))

(defn ident-from-keyword [s]
  (let [r (keyword (get (clojure.string/split (name s) #"!" 2) 1))]
    (if (nil? r)
      s
      r)))

(defn vote-tally [state]
  (let [titles-by-votes (-> (for [[title {nicks :voters}] (:votes state)] [(count nicks) title]) sort reverse)]
    (for [[votes title] titles-by-votes]
      (format "%s: %s" votes title))))

(def help [
   "Commands:"
   "!s {title} - suggest a title."
   "!v {prefix of title} - vote on a title"
   "!state - show the number of votes of the suggested titles"
   "!l {URL} - suggest a link."
   "!ll - list links"
   "!h - see this message."
   "This bot brought to you by boxed. https://github.com/boxed/atpshowbot"
])

(defn list-links [state]
  [(for [link (:links state)] (get link 0)) state])

(defn add-link [state nick string]
  ["Link added" (update-in state [:links] conj [string nick])])

(defn vote [state nick title]
  (let [x (titles-with-prefix state title)]
    (case (count x)
      0 ["No such title suggested, use !s <title> to suggest it", state]
      1 [nil (update-in state [:votes (first x) :voters] conj nick)]
      ["Multiple titles match, please be more specific" state])))

(defn suggest-title [state nick string]
  (let [title (normalize-title string)]
   (cond
    (> (count title) 74)
      ["That title is too long" state]
    (contains? (:votes state) title)
      ["Sorry, already suggested" state]
    :default
      ["Suggestion added" (assoc-in state [:votes title] {:author nick, :voters #{nick}})])))

(defn user-said-in-channel [state nick said]
  (let [[command string] (clojure.string/split said #" " 2)]
    (case command
      "!state"
        [(vote-tally state) state]
      ("!s" "!suggest")
        (if (nil? string)
          [nil state]
          (suggest-title state nick string))
      ("!h" "!help")
        [help state]
      ("!l" "!link")
        (if (nil? string)
          [nil state]
          (add-link state nick string))
      ("!v" "!vote")
        (if (nil? string)
          [nil state]
          (vote state nick (normalize-title string)))
      "!debug"
        [(format "%s" state) state]
      ("!ll" "!listlinks")
        (list-links state)
      [nil state]
     )))

(defn handle-command [state command text ident]
  (case command
    "PRIVMSG" (user-said-in-channel state ident text)
    [nil state]))


; - irclj interfacing procedures -
(defn working-reply [irc m string]
  (let [m2 (if (.startsWith (:target m) "#") m (assoc m :target (:nick m)))]
    (reply irc m2 string)))

(defn say! [irc m string]
  (if (nil? string)
    nil
    (working-reply irc m string)))

(defn callback [irc args]
  (let [{text :text target :target user :user host :host command :command} args]
    (let [[response, new_state] (handle-command @state command text (clojure.string/join "@"[user host]))]
      (if (sequential? response)
        (doseq [line response]
          (say! irc args line))
        (say! irc args response))
      (reset! state new_state))))

(defn debug-callback [irc & args]
  (prn args)
  (println))


; - Web part -
(defn app [req]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body (clojure.string/join "\n" (vote-tally @state))})

(defroutes app
  (GET "/" [] (resp/file-response "index.html" {:root "resources"}))
  (GET "/client.js" [] (resp/file-response "client.js" {:root "resources"}))
  (GET "/style.css" [] (resp/file-response "style.css" {:root "resources"}))
  (GET "/state" [] {:status 200, :headers {"Content-Type" "application/edn"}}, :body (str @state))
  (GET "/ping" [] "pong!")
  (route/not-found "<h1>Page not found</h1>"))

(defn -main [web-port]
  (println "Connecting...")
  (def irc (connect server port bot-nick :callbacks {:privmsg callback}))

  ;(identify irc bot-nick-password)

  (println "Joining")
  (join irc channel)

  (jetty/run-jetty app {:port (Integer. web-port) :join? false}))
