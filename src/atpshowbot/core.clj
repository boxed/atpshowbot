(ns atpshowbot.core
  (:require
   [irclj.core :refer :all]
   [irclj.parser :refer :all]
   [clojure.algo.generic.functor :refer :all]
   [ring.adapter.jetty :as jetty]
   ))

; - Constants -
(def channel "#qwefoobar")
(def server "irc.freenode.org")
(def port 6667)
(def bot-nick "boxed-bot")
;(def bot-nick-password "")


; - State, with example structure -
(def state (atom {:votes {}, :links []}))
;; {
;;  :votes {"title" #{:nick1 :nick2}}
;;  :links [["link" :nick1] ["another link"] :nick2]
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
   "Options:"
   "!suggest {title} - suggest a title."
   "!vote {prefix of title} - vote on a title"
   "!state - get the three most highly voted titles."
   "!link {URL} - suggest a link."
   "!help - see this message."
   "To see titles/links, go to: http://<TBI>" ; TODO
])

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
    (cond
     (= command "!state")
       [(vote-tally state) state]
     (or (= command "!s") (= command "!suggest"))
       (if (nil? string)
         [nil state]
         (suggest-title state nick string))
     (.startsWith command "!h")
       [help state]
     (.startsWith command "!l")
       (if (nil? string)
         [nil state]
         (add-link state nick string))
     (.startsWith command "!v")
       (if (nil? string)
         [nil state]
         (vote state nick (normalize-title string)))
     (.startsWith command "!d") ; debug
       [(format "%s" state) state]
     :else
       [nil state]
     )))

(defn handle-command [state command text ident]
  (case command
    "PRIVMSG" (user-said-in-channel state ident text)
    [nil state]))

; - irclj interfacing procedures -
(defn say! [irc string]
  (if (nil? string)
    nil
    (message irc channel string)))

(defn callback [irc args]
  (let [{text :text target :target user :user host :host command :command} args]
    (if (= target channel)
      (let [[response, new_state] (handle-command @state command text (clojure.string/join "@"[user host]))]
        (if (seq? response)
          (doseq [line response]
            (say! irc line))
          (say! irc response))
        (reset! state new_state)))))

(defn debug-callback [irc & args]
  (prn args)
  (println))

; - Web part -

(defn app [req]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body (clojure.string/join "<br/>" (vote-tally state))})

(defn -main [web-port]
  (println "Connecting...")
  (def irc (connect server port bot-nick :callbacks {:privmsg callback}))

  ;(identify irc bot-nick-password)

  (println "Joining")
  (join irc channel)

  (println "Starting webapp")
  (jetty/run-jetty app {:port (Integer. web-port) :join? false}))
