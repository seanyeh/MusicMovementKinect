(ns overtone-server.core
  (:require [overtone.live :refer :all]
            [overtone.inst.sampled-piano :refer :all]
            [overtone.at-at :as at-at]
            )
)


;; Global vars
(def PITCH-OFFSET (atom 0))



(defn parse-midi [filename]
  (let [notes   [(atom []),(atom [])]
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
                     (let [i      (if (= "V1" @current-voice) 0 1)
                           curvec (nth notes i)
                           ]


                       (if (not= 0 (.getDuration note))
                         (swap! curvec conj [@current-time, (.getValue note)])
                         )
                       )
                     )

                   (voiceEvent [voice]
                     (swap! current-voice (fn [x] (.getMusicString voice)))
                     )
                   )
        ]
    (.addParserListener parser listener)
    (.parse parser midiseq)
    
    notes
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

(defn play-seq [notes i sleep]
  (let [curr-i  (mod i (count notes))
        next-i  (mod (inc i) (count notes))
        curr-v  (nth notes curr-i)
        next-v  (nth notes next-i)
        ]
    (Thread/sleep (* 2 sleep))

    (sampled-piano (+ (second curr-v) @PITCH-OFFSET))

    (let [next-sleep (- (first next-v) (first curr-v))]
      (if (>= next-sleep 0)
        (play-seq notes (inc i) (- (first next-v) (first curr-v)))
        ))
    )
  )


(defn adjust-speed [msg]

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

    ;; (osc-handle server "/LEFT_HAND"   (partial ctl-bpf inst-id))
    ;; (osc-handle server "/head" on-rh-move)
    ;; (osc-handle server "/r_elbow"
    )
  )



(defn -main [& args]
  (let [notes   (parse-midi "bach1.mid")
        v1      (sort-by first @(first notes))
        v2      (sort-by first @(second notes))
        result  (merge-notes v1 v2)
        ;; thread-pool (at-at/mk-pool)
        ]
    (start-server 32000)
    (while true (play-seq result 0 0))
    )
  )




