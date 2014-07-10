(ns atpshowbot.core
  (:require
   [irclj.core :refer :all]
   [irclj.parser :refer :all]
   [clojure.algo.generic.functor :refer :all]
   [ring.adapter.jetty :as jetty]
   [compojure.route :as route]
   [compojure.core :refer [defroutes GET POST]]
   [compojure.handler :as handler]
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
;;  :votes {"title" {:voters #{"74.125.232.96" "74.125.232.96"} :author "nick1" :author-ip "74.125.232.96"}}
;;  :links [["link" "nick1" "74.125.232.96"] ["another link" "nick2" "74.125.232.96"]]
;; }


; - Pure functions -
(defn titles-with-prefix [state prefix]
  (filter #(.startsWith % prefix) (keys (:votes state))))

(defn normalize-title [title]
  (-> title
      .toLowerCase
      .trim
      (clojure.string/replace #"\.+$" "")))

(defn vote-tally [state]
  (let [titles-by-votes (-> (for [[title {ips :voters}] (:votes state)] [(count ips) title]) sort reverse)]
    (take 3
      (for [[votes title] titles-by-votes]
        (format "%s: %s" votes title)))))

(def help [
   "Commands:"
   "!s {title} - suggest a title. Votes for the title if it's already suggested."
   "!v {prefix of title} - vote on a title"
   "!state - show the number of votes of top 3 titles"
   "!l {URL} - suggest a link."
   "!ll - list links"
   "!h - see this message."
   "This bot brought to you by boxed. https://github.com/boxed/atpshowbot"
])

(defn list-links [state]
  [(for [link (:links state)] (get link 0)) state])

(defn add-link [state nick ip string]
  [nil (update-in state [:links] conj [string nick ip])])

(defn vote [state ip title]
  (let [x (titles-with-prefix state title)]
    (case (count x)
      0 ["No such title suggested, use !s <title> to suggest it", state]
      1 [nil (update-in state [:votes (first x) :voters] conj ip)]
      ["Multiple titles match, please be more specific" state])))

(defn suggest-title [state nick ip string]
  (let [title (normalize-title string)]
   (cond
    (> (count title) 74)
      ["That title is too long" state]
    (contains? (:votes state) title)
      (vote state ip title)
    :default
      [nil (assoc-in state [:votes title] {:author nick, :author-ip ip, :voters #{ip}})])))

(defn user-said-in-channel [state nick ip said]
  (let [[command string] (clojure.string/split said #" " 2)]
    (case command
      "!state"
        [(vote-tally state) state]
      ("!s" "!suggest")
        (if (nil? string)
          [nil state]
          (suggest-title state nick ip string))
      ("!h" "!help")
        [help state]
      ("!l" "!link")
        (if (nil? string)
          [nil state]
          (add-link state nick ip string))
      ("!v" "!vote")
        (if (nil? string)
          [nil state]
          (vote state ip (normalize-title string)))
      "!debug"
        (if (= nick "boxed")
          [(format "%s" state) state]
          [nil state])
      ("!ll" "!listlinks")
        (list-links state)
      [nil state]
     )))

(defn handle-command [state command text nick ip]
  (case command
    "PRIVMSG" (user-said-in-channel state nick ip text)
    [nil state]))


; - irclj interfacing procedures -
(defn working-reply [irc m string]
  (let [m2 (if (.startsWith (:target m) "#") m (assoc m :target (:nick m)))]
    (reply irc m2 string)))

(defn say! [irc m string]
  (if (nil? string)
    nil
    (working-reply irc m string)))

(defn host-to-ip [host]
  (.getHostAddress (java.net.InetAddress/getByName host)))

(defn callback [irc args]
  (let [{text :text
         target :target
         nick :nick
         host :host
         command :command} args]
    (let [[response, new_state] (handle-command @state command text nick (host-to-ip host))]
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


(defn map-function-on-map-vals [m f]
  (apply merge
         (map (fn [[k v]] {k (f v)})
              m)))

(defn count-and-did-vote [votes ip]
  (-> votes
      (assoc :votes (count (:voters votes)))
      (assoc :did-vote (contains? (:voters votes) ip))
      (dissoc :author-ip)
      (dissoc :voters)))

(defn web-state-projection [state ip]
  (pr-str
   (assoc state :votes
     (map-function-on-map-vals (:votes state)
                               #(count-and-did-vote % ip)))))

(defroutes app
  (GET "/" [] (resp/file-response "index.html" {:root "resources"}))
  (GET "/client.js" [] (resp/file-response "client.js" {:root "resources"}))
  (GET "/style.css" [] (resp/file-response "style.css" {:root "resources"}))
  (GET "/state" request {:status 200
                         :headers {"Content-Type" "application/edn"}
                         :body (web-state-projection @state (:remote-addr request))})
  (GET "/ping" [] "pong!")
  (GET "/vote" request
       (let [title (:title (:params request))
             remote-addr (:remote-addr request)]
         (reset! state (get (vote @state remote-addr title) 1))
         {:headers {"Content-Type" "application/edn"}
          :body (web-state-projection @state remote-addr)}))
  (route/not-found "<h1>Page not found</h1>"))

(defn -main [web-port]
  (println "Connecting...")
  (def irc (connect server port bot-nick :callbacks {:privmsg callback}))

  ;(identify irc bot-nick-password)

  (println "Joining")
  (join irc channel)

  (jetty/run-jetty (handler/site app) {:port (Integer. web-port) :join? false}))
