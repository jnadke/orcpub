(ns orcpub.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [orcpub.character-builder :as ch]
            [orcpub.dnd.e5.subs]
            [orcpub.dnd.e5.equipment-subs]
            [orcpub.dnd.e5.events :as events]
            [orcpub.dnd.e5.views :as views]
            [orcpub.route-map :as routes]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [clojure.string :as s]
            [re-frame.core :refer [dispatch dispatch-sync subscribe]]
            [reagent.core :as r]
            [goog.events])
  (:import
   [goog.history Html5History EventType]))

(enable-console-print!)

(if (and js/window.location
         (not (or (s/starts-with? js/window.location.href "https")
                  (s/starts-with? js/window.location.href "http://localhost"))))
  (set! js/window.location.protocol "https"))

(dispatch-sync [:initialize-db])

(def pages
  {nil ch/character-builder
   routes/default-route ch/character-builder
   routes/dnd-e5-char-builder-route ch/character-builder
   routes/dnd-e5-char-list-page-route views/character-list
   routes/dnd-e5-monster-list-page-route views/monster-list
   routes/dnd-e5-spell-list-page-route views/spell-list
   routes/dnd-e5-spell-builder-page-route views/spell-builder-page
   routes/dnd-e5-background-builder-page-route views/background-builder-page
   routes/dnd-e5-race-builder-page-route views/race-builder-page
   routes/dnd-e5-feat-builder-page-route views/feat-builder-page
   routes/dnd-e5-item-list-page-route views/item-list
   routes/dnd-e5-char-page-route views/character-page
   routes/dnd-e5-monster-page-route views/monster-page
   routes/dnd-e5-spell-page-route views/spell-page
   routes/dnd-e5-item-page-route views/item-page
   routes/dnd-e5-item-builder-page-route views/item-builder-page
   routes/dnd-e5-char-parties-page-route views/parties
   routes/dnd-e5-my-content-route views/my-content-page
   routes/register-page-route views/register-form
   routes/verify-failed-route views/verify-failed
   routes/verify-success-route views/verify-success
   routes/verify-sent-route views/verify-sent
   routes/login-page-route views/login-page
   routes/send-password-reset-page-route views/send-password-reset-page
   routes/password-reset-sent-route views/password-reset-sent
   routes/reset-password-page-route views/password-reset-page
   routes/password-reset-success-route views/password-reset-success
   routes/password-reset-expired-route views/password-reset-expired-page
   routes/password-reset-used-route views/password-reset-used-page})

(defn handle-url-change [_]
  (let [route (if js/window.location
                (routes/match-route js/window.location.pathname))
        config {:skip-path? true}]
    (dispatch [:route route (if (events/login-routes (:handler route))
                              (merge
                               config
                               {:no-return? true})
                              config)])))

(defn make-history []
  (doto (Html5History.)
    (.setPathPrefix (str js/window.location.protocol
                         "//"
                         js/window.location.host))
    (.setUseFragment false)))

(defonce history (doto (make-history)
                   (goog.events/listen EventType.NAVIGATE
                                       #(handle-url-change %))
                   (.setEnabled true)))

(defn main-view []
  (let [{:keys [handler route-params] :as route} @(subscribe [:route])
        view (pages (or handler route))]
    [view route-params]))

@(subscribe [:user false])

(r/render (if (let [doc-style js/document.documentElement.style]
                (and js/window.localStorage
                     (or (aget doc-style "flexWrap")
                         (aget doc-style "WebkitFlexWrap")
                         (aget doc-style "msFlexWrap"))))
            [main-view]
            [:div
             [views/app-header]
             [:div.f-s-24.main-text-color.sans
              {:style {:padding "200px"}}
              "Sorry, we are unable to support your browser since it does not support important HTML5 features. Please try a modern browser such as " [:a {:href "https://www.google.com/chrome/browser/desktop/index.html"} "Google Chrome"] " or " [:a {:href "https://www.mozilla.org/en-US/firefox/products/?v=a"} "Mozilla Firefox"]]])
          (js/document.getElementById "app"))
