(ns overtone-server.core
  (:require [overtone.live :refer :all]
            [overtone.inst.sampled-piano :refer :all]
            [overtone.examples.instruments.dubstep :refer :all]
            )
)

;; Global vars
(def PITCH-OFFSET (atom 0))

(def DEFAULT-SPEED 30.0)
(def SPEED (atom DEFAULT-SPEED))
(def RUNNING (atom true))

(def INSTRUMENT (atom sampled-piano))

(def D (dubstep :v 0))


(def C 48)
(def G 43)

;; Modify-notes functions
(def modify-notes identity)

(defn pentatonic [base n]
  (let [notes {0 0 1 -1
               2 0
               3 1 4 0 5 -1
               6 1 7 0 8 -1
               9 0 10 -1
               11 1}
        offset (notes (mod (+ base n) 12))]
    (+ offset n)
    ))
(def pent (partial pentatonic 0))

(defn blues [n]
  (let [notes {0 0 1 -1
               2 1 3 0 4 -1
               5 0
               6 0 7 -1
               8 -1 9 -2
               10 0 11 -1}
        offset (notes (mod n 12))]
    (+ offset n)
    ))

(defn random [n]
  (let [r (dec (rand-int 3))]
    (if (= (rand-int 2) 0)
           (+ n r)
           n
      )
    ))


;; (definst beep [note 60]
;;   (let [src (sin-osc (midicps note))
;;         env (env-gen (perc 0.01 1.0) :action :free)]
;;     (* src env)
;;     ))


(defn bpf-sweep [inst]
  (for [i (range 110)] (at (+ (now) (* i 20)) (inst i)))
  )


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

        speed-ratio (/ norm-speed DEFAULT-SPEED)

        ;; Limit max/min speed
        speed-ratio (min speed-ratio 4)
        speed-ratio (max speed-ratio 0.5)
        ]
    (if @RUNNING
      (do
        (Thread/sleep (* 2 (/ 1 speed-ratio) sleep))

        (sampled-piano (+ (modify-notes (second curr-v)) @PITCH-OFFSET))

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
    ;; (println "Speed: " speed)
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


(defn on-touch [msg]
  (let [args (:args msg)
        posx (first args)
        posy (second args)
        x (scale-num 0 550 20 80 posx)
        y (scale-num 0 1000 1 10 posy)
        ]
    (ctl D :note x)
    (ctl D :wobble y)
    ))


(defn start-server [port]
  (let [server (osc-server port "osc-clj")]
    ;; (osc-listen server (fn [msg] (println msg)) :debug)

    (osc-handle server "/LEFT_HAND"  shift-pitch)
    (osc-handle server "/RIGHT_HAND_SPEED" adjust-speed)

    (osc-handle server "/touch" on-touch)
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
    (do ((inst (first notes)) 
      (play-chord inst (drop 1 notes))
      ) 
    ))
  )

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


(defn reload []
  (use 'overtone-server.core :reload))


(defn autoreload [new-midi]
  (set-running false)
  (Thread/sleep 1000)
  (set-running true)
  (start-thread (get-notes new-midi))
  )


(defn -main [& args]
  (let [filename (or (first args) "midi/bach1.mid")]
    (println "Starting!")
    (mystart filename)
    )
  )

