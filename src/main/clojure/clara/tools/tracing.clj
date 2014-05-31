(ns clara.tools.tracing
  "Support for tracing state changes in a Clara session."
  (:require [clara.rules.listener :as l]
            [clara.rules.engine :as eng]))

(declare to-tracing-listener)

(deftype PersistentTracingListener [trace]
  l/IPersistentEventListener
  (to-transient [listener]
    (to-tracing-listener listener)))

(declare append-trace)

(deftype TracingListener [trace]
  l/ITransientEventListener
  (left-activate [listener node tokens]
    (append-trace listener {:type :left-activate :node-id (:id node) :tokens tokens}))

  (left-retract [listener node tokens]
    (append-trace listener {:type :left-retract :node-id (:id node) :tokens tokens}))

  (right-activate [listener node elements]
    (append-trace listener {:type :right-activate :node-id (:id node) :elements elements}))

  (right-retract [listener node elements]
    (append-trace listener {:type :right-retract :node-id (:id node) :elements elements}))

  (add-facts [listener facts]
    (append-trace listener {:type :add-facts :facts facts}))

  (retract-facts [listener facts]
    (append-trace listener {:type :retract-facts :facts facts}))

  (add-accum-reduced [listener node join-bindings result fact-bindings]
    (append-trace listener {:type :accum-reduced
                            :node-id (:id node)
                            :join-bindings join-bindings
                            :result result
                            :fact-bindings fact-bindings}))

  (add-activations [listener node activations]
    (append-trace listener {:type :add-activations :node-id (:id node) :activations activations}))

  (remove-activations [listener node activations]
    (append-trace listener {:type :remove-activations :node-id (:id node) :activations activations}))

  (fire-rules [listener node]
    (append-trace listener {:type :fire-rules :node-id (:id node)}))

  (send-message [listener message])

  (to-persistent [listener]
    (PersistentTracingListener. @trace)))

(defn- to-tracing-listener [^PersistentTracingListener listener]
  (TracingListener. (atom (.-trace listener))))

(defn- append-trace
  "Appends a trace event and returns a new listener with it."
  [^TracingListener listener event]
  (reset! (.-trace listener) (conj @(.-trace listener) event)))

(defn tracing-listener
  "Creates a persistent tracing event listener"
  []
  (PersistentTracingListener. []))

(defn get-trace
  "Returns the trace from the given session."
  [session]
  (if-let [listener (->> (eng/get-listeners session)
                         (filter #(instance? PersistentTracingListener %) )
                         (first))]
    (.-trace ^PersistentTracingListener listener)
    (throw (IllegalStateException. "No tracing listener attached to session."))))
