(ns overtone-server.core
  (:require [overtone.live :refer :all]
            [overtone.inst.sampled-piano :refer :all]
            [overtone.at-at :as at-at]
            )
)


;; Global vars
(def PITCH-OFFSET (atom 0))

(def DEFAULT-SPEED 50.0)
(def SPEED (atom DEFAULT-SPEED))


(defn parse-midi [filename]
  (let [
        ;; notes   [(atom []),(atom [])]
        notes   (atom [])
        current-voice   (atom "V1")
        current-time    (atom 0)
        parser  (new org.jfugue.MidiParser)
        file    (new java.io.File filename)
        midiseq (javax.sound.midi.MidiSystem/getSequence file)
        listener (proxy [org.jfugue.ParserListener] []
                   (controllerEvent [x])
                   (tempoEvent [x])
                   (timeEvent [t] 
                     (println "time" (.getTime t))
                     (swap! current-time (fn [x] (.getTime t)))
                     )
                   (instrumentEvent [x])

                   (noteEvent [note]
                     (println "note" (.getMusicString note))
                     (let [temp-v (subs @current-voice 1)
                           i      (Integer/parseInt temp-v)
                           ]
                       ; If new voice, add new voice
                       (while (>= i (count @notes)) 
                         (do (swap! notes #(conj %1 (atom []))))
                         )


                       (if (not= 0 (.getDuration note))
                         (do
                           (println "current voice:" @current-voice "temp-v" temp-v)
                           (println "i:" i "count notes:" (count @notes))
                           (swap! (nth @notes i) conj [@current-time, (.getValue note)])
                           )
                         )
                       )
                     )

                   (voiceEvent [voice]
                     (println "voice:" (.getMusicString voice))
                     (swap! current-voice (fn [x] (.getMusicString voice)))
                     )
                   )
        ]
    (.addParserListener parser listener)
    (.parse parser midiseq)
    
    @notes
    )
  )

(defn merge-notes [v1 v2]
  (let [[t1 n1]   (first v1)
        [t2 n2]   (first v2)]
    (cond (empty? v1) v2
          (empty? v2) v1
          (< t1 t2)   (cons [t1, n1] (merge-notes (drop 1 v1) v2))
          (>= t1 t2)   (cons [t2, n2] (merge-notes v1 (drop 1 v2)))
          )
    )
  )


(defn n-merge-notes[vecs]
  (if (= (count vecs) 1)
    (first vecs)
    (let [v1 (first vecs)
          v2 (second vecs)
          newvec (merge-notes v1 v2)]
      (n-merge-notes (cons newvec (drop 2 vecs)))
      )
    ))
  ;; (cond (= (count vecs) 1) (sort-by first @(first vecs))
  ;;       :else               (let [v1  (sort-by first @(first vecs))
  ;;                                 v2  (sort-by first @(second vecs))
  ;;                                 newvec (merge-notes v1 v2)
  ;;                                 ]
  ;;                             (n-merge-notes (cons newvec (drop 2 vecs)))
  ;;                             )
  ;; ))


(defn play-seq [notes i sleep]
  (let [curr-i  (mod i (count notes))
        next-i  (mod (inc i) (count notes))
        curr-v  (nth notes curr-i)
        next-v  (nth notes next-i)

        ; Avoid divide by 0
        norm-speed  (+ @SPEED 1)
        speed-ratio (/ DEFAULT-SPEED norm-speed)

        ;; Limit max speed
        speed-ratio (min speed-ratio 3.0)
        ]
    
    (Thread/sleep (* 2.5 speed-ratio sleep))

    (sampled-piano (+ (second curr-v) @PITCH-OFFSET))

    (let [next-sleep (- (first next-v) (first curr-v))]
      (if (>= next-sleep 0)
        (play-seq notes (inc i) (- (first next-v) (first curr-v)))
        ))
    )
  )


(defn adjust-speed [msg]
  (let [args  (:args msg)
        speed (map #(Math/abs %1) args)
        speed (reduce + speed)]
    (swap! SPEED (fn [x] speed))
  )
  )

(defn scale-num [inMin inMax outMin outMax x]
  (let [inNorm  (- inMax inMin)
        outNorm (- outMax outMin)
        xNorm   (- x inMin)]
    (+ (* (/ (* 1.0 xNorm) inNorm) outNorm) outMin
       )
    ))

(defn shift-pitch [msg]
  (let [args  (:args msg)
        pos   (second args)
        delta (scale-num 600 -600 4 -4 pos)
        ]
    (swap! PITCH-OFFSET (fn [x] delta))
    ))




(defn start-server [port]
  (let [server (osc-server port "osc-clj")]
    (osc-listen server (fn [msg] (println msg)) :debug)
    ;; (osc-handle server "/HAND_SPAN"   (partial ctl-reverb inst-id))

    (osc-handle server "/LEFT_HAND"  shift-pitch)
    (osc-handle server "/RIGHT_HAND_SPEED" adjust-speed)

    ;; (osc-handle server "/LEFT_HAND"   (partial ctl-bpf inst-id))
    ;; (osc-handle server "/head" on-rh-move)
    ;; (osc-handle server "/r_elbow"
    )
  )

(defn mystart [filename]
  (let [notes   (parse-midi filename)
        notes   (map (fn [x] (sort-by first @x)) notes)
        ;; v1      (sort-by first @(first notes))
        ;; v2      (sort-by first @(second notes))
        result  (n-merge-notes notes)
        ]
    (start-server 32000)

    ; Start infinite loop in new thread
    (.start (Thread. (fn []
                      (while true (play-seq result 0 0))
                    )))
    )
  )


(defn -main [& args]
  (let [filename (or (first args) "bach1.mid")]
    (println "Starting!")
    (mystart filename)
    )
  )
