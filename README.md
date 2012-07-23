[![Build Status](https://secure.travis-ci.org/flatland/ordered.png)](http://travis-ci.org/flatland/ordered)

ordered provides sets and maps that maintain the insertion order of their contents.

## Sets

    (use 'ordered.set)

    (ordered-set 4 3 1 8 2)
    => #{4 3 1 8 2}

    (conj (ordered-set 9 10) 1 2 3)
    => #{9 10 1 2 3}

    (into (ordered-set) [7 6 1 5 6])
    => #{7 6 1 5}

    (disj (ordered-set 8 1 7 2 6) 7)
    => #{8 1 2 6}

## Maps

    (use 'ordered.maps)

    (ordered-map :b 2 :a 1 :d 4)
    => {:b 2 :a 1 :d 3}

    (assoc (ordered-map :b 2 :a 1 :d 4) :c 3)
    => {:b 2 :a 1 :d 4 :c 3}

    (into (ordered-map) [[:c 3] [:a 1] [:d 4]])
    => {:c 3 :a 1 :d 4}

    (dissoc (ordered-map :c 3 :a 1 :d 4) :a)
    => {:c 3 :d 4}
