(ns clara.rules.listener
  "Event listeners for analyzing the flow through Clara.")

(defprotocol IPersistentEventListener
  (to-transient [listener]))

(defprotocol ITransientEventListener
  (left-activate [listener node tokens])
  (left-retract [listener node tokens])
  (right-activate [listener node elements])
  (right-retract [listener node elements])
  (add-facts [listener facts])
  (retract-facts [listener facts])
  (fire-rules [listener node])
  (send-message [listener message])
  (to-persistent [listener]))

;; A listener that does nothing.
(deftype NullListener []
  ITransientEventListener
  (left-activate [listener node tokens]
    listener)
  (left-retract [listener node tokens]
    listener)
  (right-activate [listener node elements]
    listener)
  (right-retract [listener node elements]
    listener)
  (add-facts [listener facts]
    listener)
  (retract-facts [listener facts]
    listener)
  (fire-rules [listener node]
    listener)
  (send-message [listener message]
    listener)
  (to-persistent [listener]
    listener)

  IPersistentEventListener
  (to-transient [listener]
    listener))

(declare delegating-listener)

;; A listener that simply delegates to others
(deftype DelegatingListener [children]
  ITransientEventListener
  (left-activate [listener node tokens]
    (doseq [child children]
      (left-activate child node tokens)))

  (left-retract [listener node tokens]
    (doseq [child children]
      (left-retract child node tokens)))

  (right-activate [listener node elements]
    (doseq [child children]
      (right-activate child node elements)))

  (right-retract [listener node elements]
    (doseq [child children]
      (right-retract child node elements)))

  (add-facts [listener facts]
    (doseq [child children]
      (add-facts child facts)))

  (retract-facts [listener facts]
    (doseq [child children]
      (retract-facts child facts)))

  (fire-rules [listener node]
    (doseq [child children]
      (fire-rules child node)))

  (send-message [listener message]
    (doseq [child children]
      (send-message child message)))

  (to-persistent [listener]
    (delegating-listener (map to-persistent children))))

(deftype PersistentDelegatingListener [children]
  IPersistentEventListener
  (to-transient [listener]
    (DelegatingListener. (map to-transient children))))

(defn delegating-listener
  "Returns a listener that delegates to its children."
  [children]
  (PersistentDelegatingListener. children))

(defn get-children
  "Returns the children of a delegating listener."
  [^PersistentDelegatingListener listener]
  (.-children listener))

;; Default listener.
(def default-listener (NullListener.))

(declare to-tracing-listener)

(deftype PersistentTracingListener [trace]
  IPersistentEventListener
  (to-transient [listener]
    (to-tracing-listener listener)))

(declare append-trace)

(deftype TracingListener [trace]
  ITransientEventListener
  (left-activate [listener node tokens]
    (append-trace listener {:type :left-activate :node-id (:id node) :tokens tokens}))

  (left-retract [listener node tokens]
    (append-trace listener {:type :left-retract :node-id (:id node) :tokens tokens}))

  (right-activate [listener node elements]
    (append-trace listener {:type :right-activate :node-id (:id node) :elements elements}))

  (right-retract [listener node elements]
    (append-trace listener {:type :right-retract :node-id (:id node) :elements elements}))

  (add-facts [listener facts]
    (println "ADDING:" facts)
    (append-trace listener {:type :add-facts :facts facts}))

  (retract-facts [listener facts]
    (append-trace listener {:type :retract-facts :facts facts}))

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
  ;; TODO: get-listeners, find the tracing listener, and return the trace...
  )


(defn get-trace [^PersistentTracingListener tracing-listener]
  (.-trace tracing-listener))
