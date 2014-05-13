(ns overtone-server.core
  (:require [overtone.live :refer :all]
            [overtone.inst.sampled-piano :refer :all]
            [overtone.examples.instruments.dubstep :refer :all]
            )
)


;; Global vars
(def PITCH-OFFSET (atom 0))

(def DEFAULT-SPEED 50.0)
(def SPEED (atom DEFAULT-SPEED))
(def RUNNING (atom true))

(def INSTRUMENT (atom sampled-piano))


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
    (if @RUNNING
      (do
        (Thread/sleep (* 2.5 speed-ratio sleep))

        (sampled-piano (+ (second curr-v) @PITCH-OFFSET))

        (let [next-sleep (- (first next-v) (first curr-v))]
          (if (>= next-sleep 0)
            (play-seq notes (inc i) (- (first next-v) (first curr-v)))
            ))
        )
      ))
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
    ;; (osc-listen server (fn [msg] (println msg)) :debug)

    (osc-handle server "/LEFT_HAND"  shift-pitch)
    (osc-handle server "/RIGHT_HAND_SPEED" adjust-speed)

    )
  )

(defn get-notes [filename]
  (let [notes   (parse-midi filename)
        notes   (map (fn [x] (sort-by first @x)) notes)
        result  (n-merge-notes notes)
        ]
    result
    )
  )

(defn play-chord [inst notes] 
  (if (> (count notes) 0) 
    (do (inst (first notes)) 
      (play-chord inst (drop 1 notes))
      ) 
    ))

(defn play-mchord [inst note] (play-chord inst [note,(+ note 3),(+ note 7)]))
(defn play-Mchord [inst note] (play-chord inst [note,(+ note 4),(+ note 7)]))

(defn start-thread [result]
    ; Start infinite loop in new thread
    (.start (Thread. (fn []
                      (while @RUNNING (play-seq result 0 0))
                    )))
  )

(defn mystart [filename]
  (start-server 32000)
  (start-thread (get-notes filename))
  )


(defn set-running [b] (swap! RUNNING (fn [x] b)))


(defn -main [& args]
  (let [filename (or (first args) "bach1.mid")]
    (println "Starting!")
    (mystart filename)
    )
  )
