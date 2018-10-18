(ns ajax-homework.core
  (:require [reagent.core :as r]
            [clojure.string :as st]
            [ajax.core :as a]
            [com.rpl.specter :as s]
            [cljs.reader :as rd]))

(declare app-state set-results save-favorites sync-favorites load-favorites add-favorite app topics add-topic-form favorites results change-gif-state)

;; app-state and mutators 

(defonce app-state
  (r/atom {:topics ["sloth" "cabbage" "motorcycle"]
           :favorites []
           :results []}))

(defn add-topic [topic]
  (->> app-state
       (s/transform [s/ATOM :topics] #(conj % topic))))

(defn set-results [response]
  "Update :results in app-state based on GIPHY response"
  (let [results (for [result (response "data")
                      :let [images (result "images")]]
                  {:id (result "id")
                   :rating (result "rating")
                   :urls {:animated (get-in images ["fixed_height" "url"])
                          :still (get-in images ["fixed_height_still" "url"])}
                   :state :still})]
    (->> app-state
         (s/setval [s/ATOM :results] results))))

(defn change-gif-state [coll-key id]
  "Flip animation state of a gif (by GIPHY id) in either favorites or results"
  (->> app-state
       (s/transform [s/ATOM coll-key s/ALL #(= id (:id %)) :state]
                    {:still :animated, :animated :still})))

;; Favorites interface

(defn set-favorites [favs]
  (save-favorites favs)
  (sync-favorites))

(defn add-favorite [fav]
  "Add a new favorite (with :state set to :still) if it isn't already present"
  (if-not (s/selected-any? [s/ALL #(= (:id %) (:id fav))] (:favorites @app-state))
    (->> fav
         (s/setval [:state] :still)
         (conj (:favorites @app-state))
         set-favorites)))

(defn remove-favorite [id]
  "Remove a favorite (by GIPHY id)"
  (->> (load-favorites)
       (s/select [s/ALL #(not= id (:id %))])
       set-favorites))

;; Favorites helpers (sync-favorites is also called once when loading)

(defn save-favorites [favs]
  "Serialize and store favorites"
  (->> (str favs)
       (.setItem js/localStorage "favorites")))

(defn load-favorites []
  "Load and deserialize favorites if present, otherwise return empty vector"
  (->> (.getItem js/localStorage "favorites")
       rd/read-string
       (into [])))

(defn sync-favorites []
  "Load favorites from localStorage into app-state"
  (->> app-state
       (s/setval [s/ATOM :favorites] (load-favorites))))

;; Ajax

(defn fetch-topic [topic]
  (a/GET "https://api.giphy.com/v1/gifs/search"
    {:params {:api_key "GVlEzWp388inZsSypxDMUpWoArWTtJO4"
              :limit 10
              :q topic}
     :handler set-results}))

;; Renderers

(defn app []
  [:div
   [topics]
   [favorites]
   [results]])

(defn topics []
  [:div.topics
   [:h1 "Topics"]
   [:ul (for [topic (:topics @app-state)]
          ^{:key topic} [:li.topic [:button {:on-click #(fetch-topic topic)} topic]])]
   [add-topic-form]])

(defn add-topic-form []
  ;; This one is stateful, so it returns a thunk which itself returns the output
  (let [val (r/atom "")
        add-topic-unless-empty #(when (seq (st/trim @val))
                                  (add-topic (st/trim @val))
                                  (reset! val ""))]
    (fn []
      [:form.add-topic
       [:input {:type "text"
                :placeholder "New Topic"
                :value @val
                :on-change #(reset! val (-> % .-target .-value))}]
       [:input {:type "submit"
                :value "Add"
                :on-click (fn [e]
                            (.preventDefault e)
                            (add-topic-unless-empty))}]])))

(defn favorites []
  (letfn [(render-favorite [fav]
            ^{:key (:id fav)}
            [:li.favorite
             [:h3 (str "Rating: " (:rating fav))]
             [:img {:src (-> fav :urls ((:state fav)))
                    :on-click #(change-gif-state :favorites (:id fav))}]
             [:button.remove-favorite {:on-click #(remove-favorite (:id fav))} "Remove"]])]
    [:div.favorites
     [:h1 "Favorites"]
     [:ul (map render-favorite (:favorites @app-state))]]))

(defn results []
  (letfn [(render-result [result]
            ^{:key (:id result)}
            [:li.result
             [:h3 (str "Rating: " (:rating result))]
             [:img {:src (-> result :urls ((:state result)))
                    :on-click #(change-gif-state :results (:id result))}]
             [:button.add-favorite {:on-click #(add-favorite result)} "Add"]])]
    [:div.results
     [:h1 "Results"]
     [:ul (map render-result (:results @app-state))]]))

;; Main

(sync-favorites)
(r/render-component [app] (. js/document (getElementById "app")))