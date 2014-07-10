(ns atpshowbot.core-test
  (:use midje.sweet)
  (:require [clojure.test :refer :all]
            [atpshowbot.core :refer :all]
            ))

(facts
  (fact
   (normalize-title "Foobar.....") => "foobar"
   (titles-with-prefix {:votes {"title" {:author :asd, :voters #{:ip1 :ip2}}}} "t") => '("title")
   (vote-tally {:votes {"title" {:voters #{:ip1 :ip2}}, "title2" {:voters #{:ip3 :ip4 :ip5}}}}) => '("3: title2" "2: title")

   (read-string (web-state-projection {:votes {"title" {:voters #{:ip1 :ip2}, :author :nick, :author-ip :ip1}, "title2" {:voters #{:ip3 :ip4 :ip5}, :author :nick2, :author-ip :ip3}}} :ip1)) =>
     {:votes {"title" {:votes 2, :did-vote true, :author :nick}, "title2" {:votes 3, :did-vote false, :author :nick2}}}

   (user-said-in-channel {} :nick :ip "!suggest title") =>
     [nil {:votes {"title" {:voters #{:ip}, :author :nick, :author-ip :ip}}}]

   (user-said-in-channel :sentinel :nick :ip "!suggest 01234567890123456789012345678901234567890123456789012345678901234567890123456") =>
     ["That title is too long" :sentinel]

   (user-said-in-channel :sentinel :nick :ip "!help") =>
     [help :sentinel]

   (user-said-in-channel {:links []} :nick :ip "!link http://foo") =>
     [nil {:links [["http://foo" :nick :ip]]}]

   (user-said-in-channel {:votes {"foo" {:voters #{:ip1}}}} :nick2 :ip2 "!vote Foo.") =>
     [nil {:votes {"foo" {:voters #{:ip1 :ip2}}}}]

   (user-said-in-channel {:votes {"foo" nil, "foobar" nil}} :nick2 :ip "!vote Foo.") =>
     ["Multiple titles match, please be more specific" {:votes {"foo" nil, "foobar" nil}}]

   (user-said-in-channel {:votes {}} :nick2 :ip "!vote foobar") =>
     ["No such title suggested, use !s <title> to suggest it" {:votes {}}]

   (user-said-in-channel {:votes {"foo" {:voters #{:ip1}}}} :nick2 :ip2 "!vote F.") =>
     [nil {:votes {"foo" {:voters #{:ip1 :ip2}}}}]

  ))
