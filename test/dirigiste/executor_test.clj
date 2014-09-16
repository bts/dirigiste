(ns dirigiste.executor-test
  (:require
    [clojure.test :refer :all])
  (:import
    [java.util
     EnumSet]
    [java.util.concurrent
     CountDownLatch
     SynchronousQueue]
    [io.aleph.dirigiste
     Executors
     Executor
     Executor$Metrics
     Stats]))

(defn run-producer [^java.util.concurrent.Executor ex n interval]
  (dotimes [_ n]
    (let [latch (CountDownLatch. 1)]
      (.execute ex
        #(do (Thread/sleep interval) (.countDown latch)))
      (.await latch))))

(defn stress-executor [ex n m interval]
  (is
    (->> (range n)
      (map (fn [_] (future (run-producer ex m interval))))
      doall
      (map #(deref % 3e5 ::timeout))
      doall
      (remove nil?)
      empty?))
  (.getStats ex))

(deftest test-executor
  (let [ex (Executors/utilization 0.9 64)]
    (try
      (is (< 30 (.getWorkerCount (stress-executor ex 32 1e6 0)) 40))
      (is (< 2 (.getWorkerCount (stress-executor ex 4 1e6 0)) 8))
      (is (< 15 (.getWorkerCount (stress-executor ex 16 1e6 0)) 20))
      (finally
        (.shutdown ex)))))