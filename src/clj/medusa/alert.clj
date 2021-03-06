(ns clj.medusa.alert
  (:require [amazonica.aws.simpleemail :as ses]
            [amazonica.core :as aws]
            [clojure.string :as string]
            [clj.medusa.config :as config]
            [clj.medusa.db :as db]
            [clj.medusa.config :as config]))

(defn send-email [subject body destinations]
  (when-not (:dry-run @config/state)
    (ses/send-email :destination {:to-addresses destinations}
                    :source "telemetry-alerts@mozilla.com"
                    :message {:subject subject
                              :body {:text body}})))

(defn notify-subscribers [{:keys [metric_id date emails]}]
  (let [{:keys [hostname]} @config/state
        foreign_subscribers (when (seq emails) (string/split emails #","))
        {metric_name :name,
         detector_id :detector_id
         metric_id :id} (db/get-metric metric_id)
        {detector_name :name} (db/get-detector detector_id)
        subscribers (db/get-subscribers-for-metric metric_id)]
    (send-email (str "Alert for " metric_name " (" detector_name ") on the " date)
                (str "http://" hostname "/index.html#/detectors/" detector_id "/"
                     "metrics/" metric_id "/alerts/?from=" date "&to=" date)
                (concat subscribers foreign_subscribers ["dev-telemetry-alerts@lists.mozilla.org"])))) 
