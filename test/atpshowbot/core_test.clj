(ns atpshowbot.core-test
  (:use midje.sweet)
  (:require [clojure.test :refer :all]
            [atpshowbot.core :refer :all]
            ))

(facts
  (fact
   (ident-from-keyword (keyword "boxed!~boxed@c-0ec6e155.13-1-64736c12.cust.bredbandsbolaget.se")) =>
     (keyword "~boxed@c-0ec6e155.13-1-64736c12.cust.bredbandsbolaget.se")

   (ident-from-keyword :foo) => :foo ; makes tests easier

   (normalize-title "Foobar.....") => "foobar"
   (titles-with-prefix {:votes {"title" {:author :asd, :voters #{:foo :bar}}}} "t") => '("title")
   (vote-tally {:votes {"title" {:voters #{:nick1 :nick2}}, "title2" {:voters #{:nick3 :nick4 :nick5}}}}) => '("3: title2" "2: title")

   (user-said-in-channel {} :nick "!suggest title") =>
     ["Suggestion added" {:votes {"title" {:voters #{:nick}, :author :nick}}}]

   (user-said-in-channel :sentinel :nick "!suggest 01234567890123456789012345678901234567890123456789012345678901234567890123456") =>
     ["That title is too long" :sentinel]

   (user-said-in-channel :sentinel :nick "!help") =>
     [help :sentinel]

   (user-said-in-channel {:links []} :nick "!link http://foo") =>
     ["Link added" {:links [["http://foo" :nick]]}]


   (user-said-in-channel {:votes {"foo" {:voters #{:nick1}}}} :nick2 "!vote Foo.") =>
     [nil {:votes {"foo" {:voters #{:nick1 :nick2}}}}]

   (user-said-in-channel {:votes {"foo" nil, "foobar" nil}} :nick2 "!vote Foo.") =>
     ["Multiple titles match, please be more specific" {:votes {"foo" nil, "foobar" nil}}]

   (user-said-in-channel {:votes {}} :nick2 "!vote foobar") =>
     ["No such title suggested, use !s <title> to suggest it" {:votes {}}]


   (user-said-in-channel {:votes {"foo" {:voters #{:nick1}}}} :nick2 "!vote F.") =>
     [nil {:votes {"foo" {:voters #{:nick1 :nick2}}}}]

   (vote {:votes {"foo" {:author "~boxed@c-0ec6e155.13-1-64736c12.cust.bredbandsbolaget.se", :voters #{"~boxed@c-0ec6e155.13-1-64736c12.cust.bredbandsbolaget.se"}}}, :links []}
         "~boxed@c-0ec6e155.13-1-64736c12.cust.bredbandsbolaget.se"
         "foo") => [nil {:links [], :votes {"foo" {:author "~boxed@c-0ec6e155.13-1-64736c12.cust.bredbandsbolaget.se", :voters #{"~boxed@c-0ec6e155.13-1-64736c12.cust.bredbandsbolaget.se"}}}}]

  ))
