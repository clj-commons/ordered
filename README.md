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

    (ordered-map :a 1 :b 2 :c 3)
    => #{4 3 1 8 2}

    (conj (ordered-set 9 10) 1 2 3)
    => #{9 10 1 2 3}

    (into (ordered-set) [7 6 1 5 6])
    => #{7 6 1 5}

    (disj (ordered-set 8 1 7 2 6) 7)
    => #{8 1 2 6}
